/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Hd
import androidx.compose.material.icons.rounded.CropRotate
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun PlayerUi(
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    isBuffering: Boolean,
    onSeekBarPositionChange: (Long) -> Unit,
    onSeekBarPositionChangeFinished: () -> Unit,
    onPlayPauseClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSeekBarVisible: Boolean = true,
    qualityIcon: ImageVector = Icons.Outlined.Hd,
    onQualityClick: (() -> Unit)? = null,
    onTransformClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center
    ) {
        if (isBuffering) {
            CircularWavyProgressIndicator(modifier = Modifier.size(20.dp))
        } else {
            IconButton(
                onClick = onPlayPauseClick, modifier = Modifier.size(100.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(
                        alpha = 0.2f
                    )
                )
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
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
                    
                    Slider(
                        value = currentPosition.toFloat(),
                        onValueChange = { onSeekBarPositionChange(it.toLong()) },
                        onValueChangeFinished = onSeekBarPositionChangeFinished,
                        valueRange = 0f..duration.toFloat(),
                        modifier = Modifier.weight(1f),
                        thumb = {
                            Box(
                                modifier = Modifier
                                    .size(15.dp)
                                    .shadow(4.dp, CircleShape)
                                    .background(Color.White, CircleShape)
                            )
                        },
                        track = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                val fraction =
                                    if (duration > 0) currentPosition.toFloat() / duration else 0f
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction)
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        })
                    
                    Text(
                        text = formatDuration(duration),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            /*
             * Кнопки правок стоят одной группой по центру: поворот слева, качество
             * справа. Каждая появляется только там, где её есть куда отправить:
             * в чате готовое видео ни сжать, ни повернуть уже нельзя.
             */
            if (onTransformClick != null || onQualityClick != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onTransformClick != null) {
                        MediaOverlayIconButton(
                            icon = Icons.Rounded.CropRotate, onClick = onTransformClick
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

fun formatDuration(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
