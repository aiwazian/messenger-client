/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

const val MIN_CONTENT_SCALE = 1f
const val MAX_CONTENT_SCALE = 4f
const val DOUBLE_TAP_CONTENT_SCALE = 2f
const val CONTENT_SCALE_TOLERANCE = 2f
const val DOUBLE_TAP_ANIMATION_DURATION = 160
const val SETTLE_ANIMATION_DURATION = 200
const val CONTENT_OVERSCROLL_FRACTION = 0.15f

private const val SCALE_THRESHOLD = 0.01f
private const val EDGE_PAN_THRESHOLD = 0.5f
private const val OVERSCROLL_SLACK = 4f

private val ZOOM_EASING = FastOutSlowInEasing

data class ZoomableLimits(
    val minScale: Float = MIN_CONTENT_SCALE,
    val maxScale: Float = MAX_CONTENT_SCALE,
    val doubleTapScale: Float = DOUBLE_TAP_CONTENT_SCALE,
    val tolerance: Float = CONTENT_SCALE_TOLERANCE,
    val doubleTapDuration: Int = DOUBLE_TAP_ANIMATION_DURATION,
    val settleDuration: Int = SETTLE_ANIMATION_DURATION,
    val overscrollFraction: Float = CONTENT_OVERSCROLL_FRACTION
)

@Stable
class ZoomableState(private val limits: ZoomableLimits) {
    
    var scale by mutableFloatStateOf(limits.minScale)
        private set
    
    private var rawOffset by mutableStateOf(Offset.Zero)
    
    val offset: Offset
        get() = Offset(rawOffset.x, dampOverscroll(rawOffset.y, maxOffset(scale).y))
    
    val isZoomed: Boolean
        get() = scale > limits.minScale + SCALE_THRESHOLD
    
    private var containerSize by mutableStateOf(Size.Zero)
    private var contentSize by mutableStateOf(Size.Zero)
    private val animationMutex = MutatorMutex()
    
    fun updateContainerSize(size: IntSize) {
        containerSize = size.toSize()
    }
    
    fun updateContentSize(size: Size) {
        contentSize = if (size.isUsable()) size else Size.Zero
    }
    
    fun applyTransform(centroid: Offset, pan: Offset, zoom: Float): Offset {
        if (!centroid.isDefined() || !pan.isDefined() || !zoom.isFinite()) {
            return Offset.Zero
        }
        
        val currentScale = scale
        val newScale = (currentScale * zoom).coerceIn(minGestureScale, maxGestureScale)
        
        if (!newScale.isFinite()) {
            return Offset.Zero
        }
        
        val requestedOffset = zoomAnchoredOffset(
            offset = rawOffset,
            anchor = centroid - containerCenter,
            pan = pan,
            scaleDelta = if (currentScale == 0f) 1f else newScale / currentScale
        )
        
        val bounds = maxOffset(newScale)
        val verticalSlack = bounds.y + overscrollDistance * OVERSCROLL_SLACK
        val allowedOffset = Offset(
            x = requestedOffset.x.coerceIn(-bounds.x, bounds.x),
            y = requestedOffset.y.coerceIn(-verticalSlack, verticalSlack)
        )
        
        scale = newScale
        rawOffset = allowedOffset
        
        return requestedOffset - allowedOffset
    }
    
    suspend fun settle() {
        if (!scale.isFinite() || !rawOffset.isDefined()) {
            scale = limits.minScale
            rawOffset = Offset.Zero
            return
        }
        
        val targetScale = scale.coerceIn(limits.minScale, limits.maxScale)
        val targetOffset = clampOffset(rawOffset, targetScale)
        
        if (targetScale == scale && targetOffset == rawOffset) {
            return
        }
        
        animateTo(targetScale, targetOffset, limits.settleDuration)
    }
    
    suspend fun toggleZoom(tapPosition: Offset) {
        if (isZoomed) {
            animateTo(limits.minScale, Offset.Zero, limits.doubleTapDuration)
            return
        }
        
        if (!tapPosition.isDefined()) {
            return
        }
        
        val targetScale = limits.doubleTapScale
        
        val targetOffset = clampOffset(
            zoomAnchoredOffset(
                offset = rawOffset,
                anchor = tapPosition - containerCenter,
                pan = Offset.Zero,
                scaleDelta = if (scale == 0f) 1f else targetScale / scale
            ), targetScale
        )
        
        animateTo(targetScale, targetOffset, limits.doubleTapDuration)
    }
    
    suspend fun reset() {
        animationMutex.mutate {
            scale = limits.minScale
            rawOffset = Offset.Zero
        }
    }
    
    private suspend fun animateTo(targetScale: Float, targetOffset: Offset, duration: Int) {
        animationMutex.mutate {
            coroutineScope {
                val scaleAnimation = Animatable(scale)
                val offsetAnimation = Animatable(rawOffset, Offset.VectorConverter)
                val scaleSpec = tween<Float>(durationMillis = duration, easing = ZOOM_EASING)
                val offsetSpec = tween<Offset>(durationMillis = duration, easing = ZOOM_EASING)
                
                launch {
                    scaleAnimation.animateTo(targetScale, scaleSpec) { scale = value }
                }
                launch {
                    offsetAnimation.animateTo(targetOffset, offsetSpec) { rawOffset = value }
                }
            }
        }
    }
    
    private fun clampOffset(offset: Offset, scale: Float): Offset {
        if (!offset.isDefined()) {
            return Offset.Zero
        }
        
        val bounds = maxOffset(scale)
        return Offset(
            x = offset.x.coerceIn(-bounds.x, bounds.x), y = offset.y.coerceIn(-bounds.y, bounds.y)
        )
    }
    
    private fun maxOffset(scale: Float): Offset {
        val content = fittedContentSize
        return Offset(
            x = ((content.width * scale - containerSize.width) / 2f).coerceAtLeast(0f),
            y = ((content.height * scale - containerSize.height) / 2f).coerceAtLeast(0f)
        )
    }
    
    private fun dampOverscroll(value: Float, bound: Float): Float {
        if (!value.isFinite()) {
            return 0f
        }
        
        val excess = abs(value) - bound
        
        if (excess <= 0f) {
            return value
        }
        
        val direction = if (value < 0f) -1f else 1f
        val limit = overscrollDistance
        
        if (limit <= 0f) {
            return direction * bound
        }
        
        return direction * (bound + limit * (1f - 1f / (excess / limit + 1f)))
    }
    
    private val fittedContentSize: Size
        get() {
            val content = contentSize
            
            if (!containerSize.isUsable() || !content.isUsable()) {
                return containerSize
            }
            
            val fitScale = minOf(
                containerSize.width / content.width, containerSize.height / content.height
            )
            
            return Size(content.width * fitScale, content.height * fitScale)
        }
    
    private val overscrollDistance: Float
        get() = containerSize.height * limits.overscrollFraction
    
    private val containerCenter: Offset
        get() = Offset(containerSize.width / 2f, containerSize.height / 2f)
    
    private val minGestureScale: Float
        get() = limits.minScale / limits.tolerance
    
    private val maxGestureScale: Float
        get() = limits.maxScale * limits.tolerance
}

@Composable
fun rememberZoomableState(limits: ZoomableLimits = ZoomableLimits()): ZoomableState {
    return remember(limits) { ZoomableState(limits) }
}

fun Modifier.zoomableContent(state: ZoomableState): Modifier = graphicsLayer {
    val contentOffset = state.offset
    
    scaleX = state.scale
    scaleY = state.scale
    translationX = contentOffset.x
    translationY = contentOffset.y
}

internal fun zoomAnchoredOffset(
    offset: Offset, anchor: Offset, pan: Offset, scaleDelta: Float
): Offset = anchor - (anchor - offset) * scaleDelta + pan

@Composable
fun Modifier.zoomableGestures(
    state: ZoomableState,
    onTap: () -> Unit = {},
    onPanBeyondEdge: (Float) -> Unit = {},
    onPanBeyondEdgeFinished: () -> Unit = {}
): Modifier {
    val scope = rememberCoroutineScope()
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnPanBeyondEdge by rememberUpdatedState(onPanBeyondEdge)
    val currentOnPanBeyondEdgeFinished by rememberUpdatedState(onPanBeyondEdgeFinished)
    
    return this
        .onSizeChanged(state::updateContainerSize)
        .pointerInput(state) {
            detectTapGestures(
                onTap = { currentOnTap() },
                onDoubleTap = { position -> scope.launch { state.toggleZoom(position) } })
        }
        .pointerInput(state) {
            awaitEachGesture {
                var pastTouchSlop = false
                var accumulatedZoom = 1f
                var accumulatedPan = Offset.Zero
                var isPanningBeyondEdge = false
                
                awaitFirstDown(requireUnconsumed = false)
                
                do {
                    val event = awaitPointerEvent()
                    
                    if (event.changes.any { it.isConsumed }) {
                        break
                    }
                    
                    val centroid = event.calculateCentroid(useCurrent = true)
                    
                    if (centroid == Offset.Unspecified) {
                        break
                    }
                    
                    val isPinch = event.changes.count { it.pressed } > 1
                    val zoomChange = event.calculateZoom()
                    val panChange = event.calculatePan()
                    
                    if (!pastTouchSlop) {
                        accumulatedZoom *= zoomChange
                        accumulatedPan += panChange
                        
                        val centroidSize = event.calculateCentroidSize(useCurrent = false)
                        val zoomMotion = abs(1f - accumulatedZoom) * centroidSize
                        val panMotion = accumulatedPan.getDistance()
                        
                        pastTouchSlop = zoomMotion > viewConfiguration.touchSlop ||
                                panMotion > viewConfiguration.touchSlop
                        
                        if (!pastTouchSlop) {
                            continue
                        }
                    }
                    
                    if (!isPinch && !state.isZoomed && !isPanningBeyondEdge) {
                        break
                    }
                    
                    if (isPanningBeyondEdge) {
                        currentOnPanBeyondEdge(panChange.x)
                    } else {
                        val unusedPan = state.applyTransform(
                            centroid = centroid, pan = panChange, zoom = zoomChange
                        )
                        
                        if (!isPinch && abs(unusedPan.x) > EDGE_PAN_THRESHOLD) {
                            isPanningBeyondEdge = true
                            currentOnPanBeyondEdge(unusedPan.x)
                        }
                    }
                    
                    event.changes.forEach { change ->
                        if (change.pressed) {
                            change.consume()
                        }
                    }
                } while (event.changes.any { it.pressed })
                
                if (isPanningBeyondEdge) {
                    currentOnPanBeyondEdgeFinished()
                }
                
                scope.launch { state.settle() }
            }
        }
}

private fun Offset.isDefined(): Boolean = x.isFinite() && y.isFinite()

private fun Size.isUsable(): Boolean =
    width.isFinite() && height.isFinite() && width > 0f && height > 0f
