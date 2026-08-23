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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

/** Как далеко надо увести содержимое, чтобы оно закрылось, в пикселях. */
const val DISMISS_DRAG_THRESHOLD = 300f

/** Прозрачность фона, пока содержимое смотрят обычно, без свайпа. */
const val DISMISS_BACKGROUND_MAX_ALPHA = 1f

/** Прозрачность фона у порога закрытия: ниже она уже не опускается. */
const val DISMISS_BACKGROUND_MIN_ALPHA = 0.2f

/**
 * Состояние вертикального свайпа, который закрывает просмотрщик.
 *
 * @param thresholdPx расстояние, после которого отпущенный палец закрывает содержимое.
 */
@Stable
class DismissDragState(val thresholdPx: Float = DISMISS_DRAG_THRESHOLD) {
    
    /** На сколько содержимое ушло за пальцем. */
    var offsetY by mutableFloatStateOf(0f)
        private set
    
    var isDragging by mutableStateOf(false)
        private set
    
    /** Насколько свайп близок к закрытию: от нуля до единицы. */
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

/**
 * Тащит содержимое за пальцем по вертикали и закрывает его, если палец отпустили
 * дальше порога.
 *
 * Жесты, которые уже кем-то забраны, пропускаются. Именно так увеличенное
 * содержимое остаётся открытым: его собственный зум забирает вертикальный свайп
 * себе, а листалка забирает горизонтальный.
 *
 * @param onTap нажатие, которое не перешло в свайп и никем не занято.
 * @param onDismiss палец отпустили дальше порога.
 */
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

/**
 * Смещает содержимое туда, куда его увёл свайп, и плавно возвращает на место, когда
 * свайпа не хватило.
 */
@Composable
fun Modifier.dismissDragOffset(state: DismissDragState): Modifier {
    val animatedOffsetY by animateFloatAsState(
        targetValue = state.offsetY, animationSpec = if (state.isDragging) snap()
        else spring(
            dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium
        ), label = "dismissDragOffsetY"
    )
    
    return this.graphicsLayer { translationY = animatedOffsetY }
}

/**
 * Прозрачность фона, который тает по мере свайпа: [maxAlpha] в покое и [minAlpha]
 * у самого порога закрытия. Совсем прозрачным фон не становится, иначе за ним
 * будет видно чёрное затемнение окна, а не цвет темы.
 */
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
