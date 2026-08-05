/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
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
const val MAX_CONTENT_SCALE = 1.8f
const val DOUBLE_TAP_CONTENT_SCALE = MAX_CONTENT_SCALE
const val CONTENT_SCALE_TOLERANCE = 1.35f

private const val SCALE_THRESHOLD = 0.01f
private const val EDGE_PAN_THRESHOLD = 0.5f

private val SCALE_ANIMATION = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow
)

private val OFFSET_ANIMATION = spring<Offset>(
    dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow
)

/**
 * Zoom limits of a [ZoomableState].
 *
 * @param minScale scale the content springs back to when it is zoomed out too far.
 * @param maxScale scale the content springs back to when it is zoomed in too far.
 * @param doubleTapScale scale a double tap applies to a content that is not zoomed yet.
 * @param tolerance how far beyond [minScale] and [maxScale] the content follows the fingers.
 */
data class ZoomableLimits(
    val minScale: Float = MIN_CONTENT_SCALE,
    val maxScale: Float = MAX_CONTENT_SCALE,
    val doubleTapScale: Float = DOUBLE_TAP_CONTENT_SCALE,
    val tolerance: Float = CONTENT_SCALE_TOLERANCE
)

/**
 * Holds the zoom and the pan of a single piece of content.
 */
@Stable
class ZoomableState(private val limits: ZoomableLimits) {
    
    var scale by mutableFloatStateOf(limits.minScale)
        private set
    
    var offset by mutableStateOf(Offset.Zero)
        private set
    
    val isZoomed: Boolean
        get() = scale > limits.minScale + SCALE_THRESHOLD
    
    private var contentSize by mutableStateOf(Size.Zero)
    private val animationMutex = MutatorMutex()
    
    fun updateContentSize(size: IntSize) {
        contentSize = size.toSize()
    }
    
    /**
     * Applies a transform gesture and returns the part of [pan] that the content
     * could not absorb because it is already at its bounds.
     */
    fun applyTransform(centroid: Offset, pan: Offset, zoom: Float): Offset {
        val currentScale = scale
        val newScale = (currentScale * zoom).coerceIn(minGestureScale, maxGestureScale)
        val scaleDelta = if (currentScale == 0f) 1f else newScale / currentScale
        val anchor = centroid - contentCenter
        val requestedOffset = anchor - (anchor - offset) * scaleDelta + pan
        val clampedOffset = clampOffset(requestedOffset, newScale)
        
        scale = newScale
        offset = clampedOffset
        
        return requestedOffset - clampedOffset
    }
    
    /**
     * Animates an over zoomed or an over panned content back into its bounds.
     */
    suspend fun settle() {
        val targetScale = scale.coerceIn(limits.minScale, limits.maxScale)
        val targetOffset = clampOffset(offset, targetScale)
        
        if (targetScale == scale && targetOffset == offset) {
            return
        }
        
        animateTo(targetScale, targetOffset)
    }
    
    /**
     * Zooms the content in around [tapPosition] or back out when it is already zoomed.
     */
    suspend fun toggleZoom(tapPosition: Offset) {
        if (isZoomed) {
            animateTo(limits.minScale, Offset.Zero)
            return
        }
        
        val targetScale = limits.doubleTapScale
        val anchor = tapPosition - contentCenter
        val scaleDelta = if (scale == 0f) 1f else targetScale / scale
        val targetOffset = clampOffset(anchor - (anchor - offset) * scaleDelta, targetScale)
        
        animateTo(targetScale, targetOffset)
    }
    
    suspend fun reset() {
        animationMutex.mutate {
            scale = limits.minScale
            offset = Offset.Zero
        }
    }
    
    private suspend fun animateTo(targetScale: Float, targetOffset: Offset) {
        animationMutex.mutate {
            coroutineScope {
                val scaleAnimation = Animatable(scale)
                val offsetAnimation = Animatable(offset, Offset.VectorConverter)
                
                launch {
                    scaleAnimation.animateTo(targetScale, SCALE_ANIMATION) { scale = value }
                }
                launch {
                    offsetAnimation.animateTo(targetOffset, OFFSET_ANIMATION) { offset = value }
                }
            }
        }
    }
    
    private fun clampOffset(offset: Offset, scale: Float): Offset {
        val bounds = maxOffset(scale)
        return Offset(
            x = offset.x.coerceIn(-bounds.x, bounds.x), y = offset.y.coerceIn(-bounds.y, bounds.y)
        )
    }
    
    private fun maxOffset(scale: Float): Offset {
        val overflow = (scale - 1f).coerceAtLeast(0f)
        return Offset(
            x = contentSize.width * overflow / 2f, y = contentSize.height * overflow / 2f
        )
    }
    
    private val contentCenter: Offset
        get() = Offset(contentSize.width / 2f, contentSize.height / 2f)
    
    private val minGestureScale: Float
        get() = limits.minScale / limits.tolerance
    
    private val maxGestureScale: Float
        get() = limits.maxScale * limits.tolerance
}

@Composable
fun rememberZoomableState(limits: ZoomableLimits = ZoomableLimits()): ZoomableState {
    return remember(limits) { ZoomableState(limits) }
}

/**
 * Draws the content with the zoom and the pan of [state].
 */
fun Modifier.zoomableContent(state: ZoomableState): Modifier = graphicsLayer {
    scaleX = state.scale
    scaleY = state.scale
    translationX = state.offset.x
    translationY = state.offset.y
}

/**
 * Detects the pinch, drag and double tap gestures that zoom and pan [state].
 *
 * Gestures that the content does not use stay unconsumed, so a parent pager keeps
 * receiving the swipes of a content that is not zoomed.
 *
 * @param onTap called for a tap that is not a part of a zoom gesture.
 * @param onPanBeyondEdge called with the horizontal drag amount that a zoomed content
 * could not absorb, which lets the caller scroll to the neighbour page.
 * @param onPanBeyondEdgeFinished called when a drag beyond the edge ends.
 */
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
        .onSizeChanged(state::updateContentSize)
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
                            centroid = event.calculateCentroid(useCurrent = true),
                            pan = panChange,
                            zoom = zoomChange
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
