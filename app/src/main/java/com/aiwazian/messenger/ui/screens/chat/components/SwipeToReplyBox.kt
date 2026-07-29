/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.screens.chat.components.SwipeToReplyDefaults.MaxOffset
import com.aiwazian.messenger.ui.screens.chat.components.SwipeToReplyDefaults.TriggerOffset
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Единственное место настройки свайпа «ответить».
 *
 * Измените [MaxOffset] или [TriggerOffset] — и поведение поменяется везде,
 * где используется [SwipeToReplyBox].
 */
object SwipeToReplyDefaults {
    /** На сколько максимум сообщение уезжает влево. */
    val MaxOffset: Dp = 60.dp
    
    /** С какого сдвига срабатывает вибрация и ответ после отпускания пальца. */
    val TriggerOffset: Dp = 40.dp
    
    /**
     * На сколько нужно вернуться ниже порога, чтобы вибрация взвелась заново.
     * Защищает от дрожания пальца ровно на границе порога.
     */
    val RearmHysteresis: Dp = 4.dp
    
    /** Насколько иконка «ответить» выезжает справа налево по ходу свайпа. */
    val IconTravel: Dp = 24.dp
    
    /** Отступ иконки от правого края. */
    val IconPadding: Dp = 16.dp
    
    /** Размер иконки. */
    val IconSize: Dp = 20.dp
}

/**
 * Оборачивает контент свайпом влево для ответа на сообщение.
 *
 * Жест устроен так:
 * 1. Ждём именно горизонтальный touch slop. Если человек ведёт палец вертикально,
 *    жест отменяется и события целиком достаются списку чата.
 * 2. Как только свайп начался, каждое событие потребляется целиком — вместе
 *    с вертикальной составляющей. Иначе LazyColumn продолжает видеть
 *    вертикальные микросдвиги пальца, копит из них скорость и дёргает
 *    чат на одно сообщение прямо во время свайпа.
 * 3. Позиция хранится в обычном float-state и применяется через graphicsLayer,
 *    поэтому на каждый кадр не запускаются корутины и не пересчитывается
 *    лейаут элемента списка.
 *
 * Отличия от `SwipeToDismissBox`: тот тянет контент на всю ширину до якоря
 * удаления и не умеет ни ограничивать сдвиг фиксированным [MaxOffset],
 * ни давать тактильную отдачу в момент пересечения порога.
 *
 * @param enabled если false — жест не ставится и иконка не рисуется.
 * @param onReply палец отпущен за порогом.
 * @param onThresholdReached каждое пересечение порога снизу вверх внутри одного жеста.
 */
@Composable
fun SwipeToReplyBox(
    enabled: Boolean,
    onReply: () -> Unit,
    onThresholdReached: () -> Unit,
    modifier: Modifier = Modifier,
    maxOffset: Dp = SwipeToReplyDefaults.MaxOffset,
    triggerOffset: Dp = SwipeToReplyDefaults.TriggerOffset,
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val maxOffsetPx = with(density) { maxOffset.toPx() }
    val triggerPx = with(density) { triggerOffset.toPx() }
    val rearmPx = with(density) { (triggerOffset - SwipeToReplyDefaults.RearmHysteresis).toPx() }
    val iconTravelPx = with(density) { SwipeToReplyDefaults.IconTravel.toPx() }
    
    var offsetPx by remember { mutableFloatStateOf(0f) }
    
    var releaseJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    
    Box(modifier = modifier.fillMaxWidth()) {
        if (enabled) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Reply,
                contentDescription = stringResource(R.string.reply),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = SwipeToReplyDefaults.IconPadding)
                    .size(SwipeToReplyDefaults.IconSize)
                    .graphicsLayer {
                        val progress = (-offsetPx / triggerPx).coerceIn(0f, 1f)
                        alpha = progress
                        translationX = (1f - progress) * iconTravelPx
                        scaleX = progress
                        scaleY = progress
                    })
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = offsetPx }
                .then(
                    if (enabled) Modifier.pointerInput(triggerPx, maxOffsetPx) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            
                            val dragStart =
                                awaitHorizontalTouchSlopOrCancellation(down.id) { change, _ ->
                                    change.consume()
                                } ?: return@awaitEachGesture
                            
                            releaseJob?.cancel()
                            
                            val pointerId = dragStart.id
                            var current = offsetPx
                            
                            /** Взведена ли вибрация: сбрасывается при возврате ниже порога. */
                            var armed = true
                            
                            while (true) {
                                val event = awaitPointerEvent()
                                val change =
                                    event.changes.firstOrNull { it.id == pointerId } ?: break
                                if (!change.pressed) break
                                
                                current = (current + change.positionChange().x)
                                    .coerceIn(-maxOffsetPx, 0f)
                                offsetPx = current
                                
                                change.consume()
                                
                                val distance = -current
                                if (distance >= triggerPx) {
                                    if (armed) {
                                        armed = false
                                        onThresholdReached()
                                    }
                                } else if (distance < rearmPx) {
                                    armed = true
                                }
                            }
                            
                            val shouldReply = -offsetPx >= triggerPx
                            releaseJob = scope.launch {
                                animate(initialValue = offsetPx, targetValue = 0f) { value, _ ->
                                    offsetPx = value
                                }
                            }
                            if (shouldReply) onReply()
                        }
                    } else Modifier
                ),
            content = content
        )
    }
}
