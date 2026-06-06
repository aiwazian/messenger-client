/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiwazian.messenger.domain.MessageAttachment
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.enums.FileAction
import com.aiwazian.messenger.ui.components.formatDuration
import com.aiwazian.messenger.utils.AmplitudeExtractor
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun MessageVoice(
    file: MessageAttachment,
    isPlaying: Boolean,
    positionMs: Int,
    durationMs: Int,
    onAction: (FileAction) -> Unit,
    onSeek: (Int) -> Unit
) {
    val context = LocalContext.current
    val isReady = file.localUri != null
    
    var amplitudes by remember { mutableStateOf<List<Float>?>(null) }
    var dragPositionMs by remember { mutableStateOf<Int?>(null) }
    
    val currentPositionMs by rememberUpdatedState(positionMs)
    val currentDurationMs by rememberUpdatedState(durationMs)
    val currentOnSeek by rememberUpdatedState(onSeek)
    var initialSeekPositionMs by remember { mutableStateOf<Int?>(null) }
    
    LaunchedEffect(isPlaying) {
        if (isPlaying && initialSeekPositionMs != null) {
            delay(100.milliseconds)
            currentOnSeek(initialSeekPositionMs!!)
            initialSeekPositionMs = null
        } else if (isPlaying) {
            initialSeekPositionMs = null
        }
    }
    
    val effectiveMs = dragPositionMs ?: initialSeekPositionMs ?: currentPositionMs
    
    var extractedDurationMs by remember { mutableIntStateOf(0) }
    LaunchedEffect(file.localUri) {
        if (file.localUri != null) {
            if (amplitudes == null) {
                amplitudes = AmplitudeExtractor.extract(context, file.localUri)
            }
            if (durationMs == 0 && extractedDurationMs == 0) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val retriever = android.media.MediaMetadataRetriever()
                        retriever.setDataSource(context, file.localUri)
                        val durationStr =
                            retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                        extractedDurationMs = durationStr?.toIntOrNull() ?: 0
                        retriever.release()
                    } catch (e: Exception) {
                        Log.e("MessageVoice", "LaunchedEffect error ${e.message}", e)
                    }
                }
            }
        }
    }
    
    val finalDurationMs = if (currentDurationMs > 0) currentDurationMs else extractedDurationMs
    val finalDurationToUse = if (finalDurationMs > 0) finalDurationMs else 1
    val finalProgress = effectiveMs.toFloat() / finalDurationToUse
    
    Row(
        modifier = Modifier
            .clickable(interactionSource = null, indication = null) {
                if (file.status == DownloadStatus.DOWNLOADING || file.status == DownloadStatus.UPLOADING) {
                    onAction(FileAction.CANCEL)
                } else if (file.status == DownloadStatus.COMPLETED || isReady) {
                    onAction(FileAction.PLAY)
                } else {
                    onAction(FileAction.DOWNLOAD)
                }
            }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            if (file.status == DownloadStatus.DOWNLOADING || file.status == DownloadStatus.UPLOADING) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Cancel",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                if (file.progress == 0) {
                    CircularWavyProgressIndicator(
                        modifier = Modifier.size(48.dp)
                    )
                } else {
                    val animatedProgress by animateFloatAsState(
                        targetValue = file.progress / 100f,
                        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                    )
                    
                    CircularWavyProgressIndicator(
                        progress = { animatedProgress }, modifier = Modifier.size(48.dp)
                    )
                }
            } else if (isReady) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = if (file.status == DownloadStatus.FAILED) Icons.Rounded.Refresh else Icons.Rounded.Download,
                    contentDescription = file.status.name,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .pointerInput(finalDurationToUse) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                val downX = offset.x.coerceIn(0f, size.width.toFloat())
                                dragPositionMs = if (size.width > 0) {
                                    (downX / size.width * finalDurationToUse).toInt()
                                        .coerceIn(0, finalDurationToUse)
                                } else 0
                            },
                            onDragEnd = {
                                if (isPlaying) {
                                    dragPositionMs?.let {
                                        currentOnSeek(it)
                                    }
                                } else {
                                    initialSeekPositionMs = dragPositionMs
                                }
                                dragPositionMs = null
                            },
                            onDragCancel = {
                                dragPositionMs = null
                            },
                            onHorizontalDrag = { change, _ ->
                                change.consume()
                                val currentX = change.position.x.coerceIn(0f, size.width.toFloat())
                                dragPositionMs = if (size.width > 0) {
                                    (currentX / size.width * finalDurationToUse).toInt()
                                        .coerceIn(0, finalDurationToUse)
                                } else 0
                            }
                        )
                    }
            ) {
                Waveform(
                    amplitudes = amplitudes ?: emptyList(),
                    progress = finalProgress,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Text(
                text = formatDuration(effectiveMs.toLong()),
                fontSize = 12.sp,
                lineHeight = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Waveform(
    amplitudes: List<Float>,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    val placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
    
    Canvas(modifier = modifier) {
        val usePlaceholder = amplitudes.isEmpty()
        val barCount = if (usePlaceholder) AmplitudeExtractor.AMPLITUDES_COUNT else amplitudes.size
        val barWidth = size.width / barCount
        val centerY = size.height / 2f
        val progressX = size.width * progress.coerceIn(0f, 1f)
        val placeholderHeight = (size.height * 0.3f).coerceAtLeast(2f)
        
        for (i in 0 until barCount) {
            val barHeight = if (usePlaceholder) {
                placeholderHeight
            } else {
                (size.height * amplitudes[i]).coerceAtLeast(2f)
            }
            val x = i * barWidth
            val color = when {
                x < progressX -> activeColor
                usePlaceholder -> placeholderColor
                else -> inactiveColor
            }
            drawRoundRect(
                color = color,
                topLeft = Offset(x + barWidth * 0.25f, centerY - barHeight / 2f),
                size = Size(barWidth * 0.5f, barHeight),
                cornerRadius = CornerRadius(barWidth / 4f, barWidth / 4f)
            )
        }
    }
}
