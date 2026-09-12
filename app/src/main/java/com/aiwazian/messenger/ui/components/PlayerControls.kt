/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Hd
import androidx.compose.material.icons.rounded.CropRotate
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.ui.compose.material3.indicator.ProgressSlider
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import androidx.media3.ui.compose.state.rememberProgressStateWithTickInterval
import kotlin.math.abs
import kotlin.math.roundToInt

private const val PROGRESS_TICK_INTERVAL_MS = 1_000L

@Composable
fun PlayerCenterControls(
    player: Player?,
    showControls: Boolean,
    modifier: Modifier = Modifier,
    isBuffering: Boolean = false
) {
    val playPauseButtonState = rememberPlayPauseButtonState(player)
    
    val containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.2f)
    
    AnimatedVisibility(
        visible = showControls, modifier = modifier, enter = fadeIn(), exit = fadeOut()
    ) {
        if (isBuffering) {
            CircularWavyProgressIndicator(modifier = Modifier.size(20.dp))
        } else {
            IconButton(
                onClick = playPauseButtonState::onClick,
                modifier = Modifier.size(100.dp),
                enabled = playPauseButtonState.isEnabled,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = containerColor, disabledContainerColor = containerColor
                )
            ) {
                Icon(
                    imageVector = if (playPauseButtonState.showPlay) {
                        Icons.Rounded.PlayArrow
                    } else {
                        Icons.Rounded.Pause
                    },
                    contentDescription = if (playPauseButtonState.showPlay) "Play" else "Pause",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

@Composable
fun PlayerBottomControls(
    player: Player?,
    showControls: Boolean,
    modifier: Modifier = Modifier,
    isSeekBarVisible: Boolean = true,
    qualityIcon: ImageVector = Icons.Outlined.Hd,
    isTransformed: Boolean = false,
    onQualityClick: (() -> Unit)? = null,
    onTransformClick: (() -> Unit)? = null
) {
    val progressState = rememberProgressStateWithTickInterval(
        player = player, tickIntervalMs = PROGRESS_TICK_INTERVAL_MS
    )
    
    var seekProgress by remember { mutableStateOf<Float?>(null) }
    
    val duration = progressState.durationMs.takeIf { it != C.TIME_UNSET }?.coerceAtLeast(0L) ?: 0L
    
    val currentPosition = seekProgress?.let { (it * duration).toLong() }
        ?: progressState.currentPositionMs
    
    AnimatedVisibility(
        visible = showControls, modifier = modifier, enter = fadeIn(), exit = fadeOut()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isSeekBarVisible) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatDuration(currentPosition),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Box(modifier = Modifier.weight(1f)) {
                        ProgressSlider(
                            player = player,
                            modifier = Modifier.fillMaxWidth(),
                            onValueChange = { progress -> seekProgress = progress },
                            onValueChangeFinished = { seekProgress = null }
                        )
                    }
                    
                    Text(
                        text = formatDuration(duration),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            if (onTransformClick != null || onQualityClick != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onTransformClick != null) {
                        MediaOverlayIconButton(
                            icon = Icons.Rounded.CropRotate,
                            onClick = onTransformClick,
                            isActive = isTransformed
                        )
                    }
                    
                    if (onQualityClick != null) {
                        MediaOverlayIconButton(icon = qualityIcon, onClick = onQualityClick)
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerSeekIndicator(
    seekAmountMs: Long,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible, modifier = modifier, enter = fadeIn(), exit = fadeOut()
    ) {
        Row(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f),
                    shape = CircleShape
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (seekAmountMs < 0) {
                    Icons.Rounded.FastRewind
                } else {
                    Icons.Rounded.FastForward
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
            
            Text(text = formatSeekAmount(seekAmountMs), color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun PlayerSpeedBadge(
    speed: Float,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible, modifier = modifier, enter = fadeIn(), exit = fadeOut()
    ) {
        Row(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f),
                    shape = CircleShape
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.FastForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
            
            Text(text = formatPlaybackSpeed(speed), color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

private fun formatSeekAmount(amountMs: Long): String {
    val seconds = abs(amountMs) / 1_000L
    val sign = if (amountMs < 0) "-" else "+"
    
    return "$sign$seconds с"
}

private fun formatPlaybackSpeed(speed: Float): String {
    val rounded = (speed * 10f).roundToInt() / 10f
    
    return if (rounded % 1f == 0f) {
        "${rounded.toInt()}x"
    } else {
        "${rounded}x"
    }
}
