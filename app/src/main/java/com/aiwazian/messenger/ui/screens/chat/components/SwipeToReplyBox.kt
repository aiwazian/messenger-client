/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.screens.chat.components.SwipeToReplyDefaults.MaxOffset
import com.aiwazian.messenger.ui.screens.chat.components.SwipeToReplyDefaults.TriggerOffset
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
 * Отличия от `SwipeToDismissBox`: тот тянет контент на всю ширину до якоря
 * удаления и не умеет ни ограничивать сдвиг фиксированными [SwipeToReplyDefaults.MaxOffset],
 * ни давать тактильную отдачу в момент пересечения порога без отпускания пальца.
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
    
    /** Сдвиг контента: отрицательный, потому что тянем влево. */
    val offset = remember { Animatable(0f) }
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
                        // Прогресс свайпа до порога: иконка проявляется и едет справа налево.
                        val progress = (-offset.value / triggerPx).coerceIn(0f, 1f)
                        alpha = progress
                        translationX = (1f - progress) * iconTravelPx
                        scaleX = progress
                        scaleY = progress
                    })
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .then(
                    if (enabled) Modifier.pointerInput(triggerPx, maxOffsetPx) {
                        /** Взведена ли вибрация: сбрасывается при возврате ниже порога. */
                        var armed = true
                        detectHorizontalDragGestures(
                            onDragStart = { armed = true },
                            onDragEnd = {
                                val shouldReply = -offset.value >= triggerPx
                                scope.launch { offset.animateTo(0f) }
                                if (shouldReply) onReply()
                            },
                            onDragCancel = {
                                scope.launch { offset.animateTo(0f) }
                            }) { _, dragAmount ->
                            val target = (offset.value + dragAmount).coerceIn(-maxOffsetPx, 0f)
                            scope.launch { offset.snapTo(target) }
                            
                            val distance = -target
                            when {
                                distance >= triggerPx && armed -> {
                                    armed = false
                                    onThresholdReached()
                                }
                                
                                distance < rearmPx -> armed = true
                            }
                        }
                    } else Modifier
                ),
            content = content
        )
    }
}
