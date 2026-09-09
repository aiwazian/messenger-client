/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

/** Как далеко надо увести содержимое, чтобы оно закрылось, в пикселях. */
const val DISMISS_DRAG_THRESHOLD = 300f

/** Прозрачность фона, пока содержимое смотрят обычно, без свайпа. */
const val DISMISS_BACKGROUND_MAX_ALPHA = 1f

/** Прозрачность фона у порога закрытия: ниже она уже не опускается. */
const val DISMISS_BACKGROUND_MIN_ALPHA = 0.2f

@Stable
class DismissDragState(val thresholdPx: Float = DISMISS_DRAG_THRESHOLD) {
    
    var offsetY by mutableFloatStateOf(0f)
        private set
    
    var isDragging by mutableStateOf(false)
        private set
    
    val progress: Float
        get() = (abs(offsetY) / thresholdPx).coerceIn(0f, 1f)
    
    internal fun onDrag(offsetY: Float) {
        isDragging = true
        this.offsetY = offsetY
    }
    
    internal fun onDragReturn() {
        isDragging = false
        offsetY = 0f
    }
    
    internal fun onGestureEnd() {
        isDragging = false
    }
}

@Composable
fun rememberDismissDragState(thresholdPx: Float = DISMISS_DRAG_THRESHOLD): DismissDragState {
    return remember(thresholdPx) { DismissDragState(thresholdPx) }
}

@Composable
fun Modifier.dismissDragGestures(
    state: DismissDragState, onTap: () -> Unit = {}, onDismiss: () -> Unit
): Modifier {
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    
    return this.pointerInput(state) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val pointerId = down.id
            
            var previousY = down.position.y
            var dragDetected = false
            var totalDragY = 0f
            var totalDragX: Float
            var isHorizontalScroll = false
            var wasConsumed = false
            
            while (true) {
                val event = awaitPointerEvent()
                
                val change =
                    event.changes.firstOrNull { it.id == pointerId } ?: event.changes.firstOrNull()
                    ?: break
                
                if (change.isConsumed) {
                    wasConsumed = true
                    previousY = change.position.y
                    if (!change.pressed) break
                    continue
                }
                
                val dy = change.position.y - previousY
                val dx = change.position.x - down.position.x
                previousY = change.position.y
                
                if (!dragDetected && !isHorizontalScroll) {
                    totalDragY += dy
                    totalDragX = dx
                    
                    if (abs(totalDragX) > viewConfiguration.touchSlop && abs(totalDragX) > abs(
                            totalDragY
                        )
                    ) {
                        isHorizontalScroll = true
                    } else if (abs(totalDragY) > viewConfiguration.touchSlop) {
                        dragDetected = true
                        state.onDrag(totalDragY)
                        change.consume()
                    }
                } else if (dragDetected) {
                    change.consume()
                    totalDragY += dy
                    state.onDrag(totalDragY)
                }
                
                if (!change.pressed) break
            }
            
            when {
                !dragDetected && !isHorizontalScroll && !wasConsumed -> {
                    currentOnTap()
                }
                
                abs(totalDragY) > state.thresholdPx && dragDetected -> {
                    currentOnDismiss()
                }
                
                else -> {
                    state.onDragReturn()
                }
            }
            
            state.onGestureEnd()
        }
    }
}

@Composable
fun DismissDragState.animatedOffsetY(): Float {
    val animatedOffsetY by animateFloatAsState(
        targetValue = offsetY, animationSpec = if (isDragging) snap()
        else spring(
            dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium
        ), label = "dismissDragOffsetY"
    )
    
    return animatedOffsetY
}

@Composable
fun DismissDragState.animatedBackgroundAlpha(
    maxAlpha: Float = DISMISS_BACKGROUND_MAX_ALPHA,
    minAlpha: Float = DISMISS_BACKGROUND_MIN_ALPHA
): Float {
    val alpha by animateFloatAsState(
        targetValue = maxAlpha - (maxAlpha - minAlpha) * progress,
        label = "dismissDragBackgroundAlpha"
    )
    
    return alpha
}
