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

object SwipeToReplyDefaults {
    val MaxOffset: Dp = 60.dp
    
    val TriggerOffset: Dp = 40.dp
    
    val RearmHysteresis: Dp = 4.dp
    
    val IconTravel: Dp = 24.dp
    
    val IconPadding: Dp = 16.dp
    
    val IconSize: Dp = 20.dp
}

@Composable
fun SwipeToReplyBox(
    enabled: Boolean,
    onReply: () -> Unit,
    onThresholdReached: () -> Unit,
    modifier: Modifier = Modifier,
    maxOffset: Dp = MaxOffset,
    triggerOffset: Dp = TriggerOffset,
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
