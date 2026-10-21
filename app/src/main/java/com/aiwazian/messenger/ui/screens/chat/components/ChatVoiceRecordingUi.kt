/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.screens.chat.ChatUiState
import kotlin.math.abs

@Composable
internal fun VoiceRecordingStatus(
    uiState: ChatUiState, micTranslationX: Float, onCancelRecording: () -> Unit
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = uiState.isRecording,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val durationText = String.format(
                LocalLocale.current.platformLocale,
                "%02d:%02d",
                uiState.recordingDurationMs / 1000 / 60,
                uiState.recordingDurationMs / 1000 % 60
            )

            Text(durationText, style = MaterialTheme.typography.bodyLarge)

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center
            ) {
                AnimatedContent(
                    targetState = uiState.isRecordingLocked, transitionSpec = {
                        slideInVertically { -it } + fadeIn() togetherWith slideOutVertically { it } + fadeOut()
                    }, label = "recording_hint_animation", contentAlignment = Alignment.Center
                ) { isLocked ->
                    if (isLocked) {
                        TextButton(onClick = onCancelRecording) {
                            Text(stringResource(R.string.cancel).uppercase())
                        }
                    } else {
                        val infiniteTransition = rememberInfiniteTransition(label = "shake")

                        val offsetX by infiniteTransition.animateFloat(
                            initialValue = -4f,
                            targetValue = 4f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(
                                    durationMillis = 1000, easing = LinearEasing
                                ), repeatMode = RepeatMode.Reverse
                            ),
                            label = "offsetX"
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .graphicsLayer {
                                    translationX = micTranslationX * 0.5f
                                    alpha = (1f - (abs(micTranslationX) / 250f)).coerceIn(
                                        0.2f, 1f
                                    )
                                }
                                .offset {
                                    IntOffset(x = offsetX.dp.roundToPx(), y = 0)
                                }
                                .padding(horizontal = 2.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowBackIosNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.swipe_to_cancel),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun VoiceRecordingLockedIcon(
    uiState: ChatUiState, micTranslationY: Float
) {
    val micIconPosition by animateFloatAsState(
        targetValue = -70f + if (!uiState.isRecordingLocked) {
            micTranslationY * 0.3f
        } else {
            0f
        }
    )

    androidx.compose.animation.AnimatedVisibility(
        visible = uiState.isRecording,
        enter = fadeIn() + scaleIn() + slideInVertically { it / 2 },
        exit = fadeOut() + scaleOut() + slideOutVertically { it / 2 },
        modifier = Modifier.offset {
            IntOffset(
                x = 0, y = micIconPosition.dp.roundToPx()
            )
        }) {
        val isNearLock = uiState.isRecordingLocked || micTranslationY < -150f
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(8.dp)
        ) {
            AnimatedContent(
                targetState = isNearLock,
                transitionSpec = { fadeIn() togetherWith fadeOut() }) { isNearLock ->
                Icon(
                    imageVector = if (isNearLock) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
internal fun VoiceRecordingAmplitudeEffect(amplitude: Float) {
    val maxBackgroundScale = 2.2f
    val currentScale = 1f + ((amplitude * 2.5f).coerceAtMost(1f) * (maxBackgroundScale - 1f))
    Box(
        modifier = Modifier
            .size(48.dp)
            .graphicsLayer {
                scaleX = currentScale
                scaleY = currentScale
            }
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)))
}
