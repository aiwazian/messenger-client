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
const val MAX_CONTENT_SCALE = 2f
const val DOUBLE_TAP_CONTENT_SCALE = 1.5f
const val CONTENT_SCALE_TOLERANCE = 1.35f

/** Duration of the zoom of a double tap, in milliseconds. */
const val DOUBLE_TAP_ANIMATION_DURATION = 160

/** Duration of the animation that brings the content back into its bounds, in milliseconds. */
const val SETTLE_ANIMATION_DURATION = 200

/**
 * How far past its vertical bounds the content follows the fingers, as a part of
 * the height of the viewport.
 */
const val CONTENT_OVERSCROLL_FRACTION = 0.15f

private const val SCALE_THRESHOLD = 0.01f
private const val EDGE_PAN_THRESHOLD = 0.5f

/**
 * How much pan past the vertical bounds the state keeps, in overscroll distances.
 *
 * Dragging further than that stops moving the content at all, which is what makes
 * the resistance feel like an end of the content and not like a slower drag.
 */
private const val OVERSCROLL_SLACK = 4f

private val ZOOM_EASING = FastOutSlowInEasing

/**
 * Zoom limits of a [ZoomableState].
 *
 * @param minScale scale the content springs back to when it is zoomed out too far.
 * @param maxScale scale the content springs back to when it is zoomed in too far.
 * @param doubleTapScale scale a double tap applies to a content that is not zoomed yet.
 * @param tolerance how far beyond [minScale] and [maxScale] the content follows the fingers.
 * @param doubleTapDuration duration of the zoom of a double tap, in milliseconds.
 * @param settleDuration duration of the return into the bounds, in milliseconds.
 * @param overscrollFraction how far past its vertical bounds the content follows the
 * fingers, as a part of the height of the viewport.
 */
data class ZoomableLimits(
    val minScale: Float = MIN_CONTENT_SCALE,
    val maxScale: Float = MAX_CONTENT_SCALE,
    val doubleTapScale: Float = DOUBLE_TAP_CONTENT_SCALE,
    val tolerance: Float = CONTENT_SCALE_TOLERANCE,
    val doubleTapDuration: Int = DOUBLE_TAP_ANIMATION_DURATION,
    val settleDuration: Int = SETTLE_ANIMATION_DURATION,
    val overscrollFraction: Float = CONTENT_OVERSCROLL_FRACTION
)

/**
 * Holds the zoom and the pan of a single piece of content.
 *
 * The state knows two sizes: the viewport, reported by [updateContainerSize], and
 * the content itself, reported by [updateContentSize]. Only the second one tells
 * how much of the viewport the content really covers, and therefore how far it may
 * be panned: a wide photo keeps empty bands above and below itself even when it is
 * zoomed, and there is nothing to show there.
 */
@Stable
class ZoomableState(private val limits: ZoomableLimits) {
    
    var scale by mutableFloatStateOf(limits.minScale)
        private set
    
    /**
     * Pan the gestures accumulated. It may point past the vertical bounds: that
     * part is damped by [offset] instead of being cut off, so a content that
     * cannot move any further still follows the finger a little.
     */
    private var rawOffset by mutableStateOf(Offset.Zero)
    
    /** Pan the content is drawn with. */
    val offset: Offset
        get() = Offset(rawOffset.x, dampOverscroll(rawOffset.y, maxOffset(scale).y))
    
    val isZoomed: Boolean
        get() = scale > limits.minScale + SCALE_THRESHOLD
    
    private var containerSize by mutableStateOf(Size.Zero)
    private var contentSize by mutableStateOf(Size.Zero)
    private val animationMutex = MutatorMutex()
    
    /** Reports the size of the viewport the content is drawn in. */
    fun updateContainerSize(size: IntSize) {
        containerSize = size.toSize()
    }
    
    /**
     * Reports the size the content has on its own. The unit does not matter, only
     * the ratio of the sides is used, so the size of a bitmap or of a video frame
     * both work.
     */
    fun updateContentSize(size: Size) {
        contentSize = if (size.isUsable()) size else Size.Zero
    }
    
    /**
     * Applies a transform gesture and returns the part of [pan] that the content
     * could not absorb because it is already at its bounds.
     *
     * Horizontally the pan is cut off at the bound, and the rest is handed back to
     * the caller, which turns it into a page change. Vertically it is kept instead,
     * damped by [offset]: a swipe of a zoomed content has to move the content a
     * little and let it spring back, not to close the viewer.
     *
     * A gesture that reports an undefined centroid, a pan or a zoom is ignored:
     * such a value turns the whole transform into a not a number one and hides
     * the content until the state is recreated.
     */
    fun applyTransform(centroid: Offset, pan: Offset, zoom: Float): Offset {
        if (!centroid.isDefined() || !pan.isDefined() || !zoom.isFinite()) {
            return Offset.Zero
        }
        
        val currentScale = scale
        val newScale = (currentScale * zoom).coerceIn(minGestureScale, maxGestureScale)
        
        if (!newScale.isFinite()) {
            return Offset.Zero
        }
        
        val scaleDelta = if (currentScale == 0f) 1f else newScale / currentScale
        val anchor = centroid - containerCenter
        val requestedOffset = anchor - (anchor - rawOffset) * scaleDelta + pan
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
    
    /**
     * Animates an over zoomed or an over panned content back into its bounds.
     *
     * A content that has no room to move vertically comes back to the centre, and
     * a content that is taller than the viewport comes back to the edge it was
     * pulled away from.
     */
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
    
    /**
     * Zooms the content in around [tapPosition] or back out when it is already zoomed.
     */
    suspend fun toggleZoom(tapPosition: Offset) {
        if (isZoomed) {
            animateTo(limits.minScale, Offset.Zero, limits.doubleTapDuration)
            return
        }
        
        if (!tapPosition.isDefined()) {
            return
        }
        
        val targetScale = limits.doubleTapScale
        val anchor = tapPosition - containerCenter
        val scaleDelta = if (scale == 0f) 1f else targetScale / scale
        val targetOffset = clampOffset(anchor - (anchor - rawOffset) * scaleDelta, targetScale)
        
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
    
    /**
     * Half of the room the content has to move in, per axis.
     *
     * It is zero on an axis the content does not overflow, and a pan along such an
     * axis only gets the damped movement of [dampOverscroll].
     */
    private fun maxOffset(scale: Float): Offset {
        val content = fittedContentSize
        return Offset(
            x = ((content.width * scale - containerSize.width) / 2f).coerceAtLeast(0f),
            y = ((content.height * scale - containerSize.height) / 2f).coerceAtLeast(0f)
        )
    }
    
    /**
     * Damps the part of [value] that is past [bound]: the content keeps following
     * the finger, but slower and slower, and never gets further than one overscroll
     * distance away from its bound.
     */
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
    
    /**
     * Size the content covers inside the viewport before the zoom.
     *
     * The viewers draw with ContentScale.Fit, so the content keeps the ratio of its
     * sides and one of them stays shorter than the viewport. Until the size of the
     * content is known the viewport is used instead, which is the size a content
     * that fills it exactly would have.
     */
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

/**
 * Draws the content with the zoom and the pan of [state].
 */
fun Modifier.zoomableContent(state: ZoomableState): Modifier = graphicsLayer {
    val contentOffset = state.offset
    
    scaleX = state.scale
    scaleY = state.scale
    translationX = contentOffset.x
    translationY = contentOffset.y
}

/**
 * Detects the pinch, drag and double tap gestures that zoom and pan [state].
 *
 * Gestures that the content does not use stay unconsumed, so a parent pager keeps
 * receiving the swipes of a content that is not zoomed. A zoomed content, on the
 * other hand, consumes them, which is what keeps a vertical swipe from closing
 * the viewer.
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
                    
                    /*
                     * The centroid of an event that lifts the last finger is
                     * unspecified, and transforming the content with it makes the
                     * content disappear.
                     */
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
