/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import android.media.MediaPlayer
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
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
    onAction: (FileAction) -> Unit
) {
    val context = LocalContext.current
    val isReady = file.localUri != null
    
    var amplitudes by remember { mutableStateOf<List<Float>?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var durationMs by remember { mutableIntStateOf(0) }
    var positionMs by remember { mutableIntStateOf(0) }
    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetMs by remember { mutableFloatStateOf(0f) }
    
    val player = remember { MediaPlayer() }
    
    LaunchedEffect(file.localUri) {
        if (file.localUri != null && amplitudes == null) {
            amplitudes = AmplitudeExtractor.extract(context, file.localUri)
        }
    }
    
    LaunchedEffect(file.localUri) {
        if (file.localUri != null) {
            runCatching {
                player.reset()
                player.setDataSource(context, file.localUri)
                player.setOnPreparedListener { mp ->
                    durationMs = mp.duration
                }
                player.setOnCompletionListener {
                    isPlaying = false
                    positionMs = 0
                    it.seekTo(0)
                }
                player.prepareAsync()
            }
        }
    }
    
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            positionMs = player.currentPosition
            delay(50.milliseconds)
        }
    }
    
    DisposableEffect(Unit) { onDispose { player.release() } }
    
    val effectiveMs = (positionMs + dragOffsetMs.toInt())
        .coerceIn(0, durationMs.coerceAtLeast(1))
    val progress = if (durationMs > 0) effectiveMs.toFloat() / durationMs else 0f
    
    Row(
        modifier = Modifier
            .clickable {
                if (file.localUri != null) {
                    onAction(FileAction.OPEN)
                    return@clickable
                }
                
                val action = when (file.status) {
                    DownloadStatus.DOWNLOADING -> FileAction.PAUSE
                    DownloadStatus.PAUSED -> FileAction.DOWNLOAD
                    DownloadStatus.IDLE,
                    DownloadStatus.CANCELLED,
                    DownloadStatus.FAILED,
                    DownloadStatus.UPLOADED -> FileAction.DOWNLOAD
                    
                    DownloadStatus.UPLOADING -> FileAction.CANCEL
                    DownloadStatus.COMPLETED -> FileAction.OPEN
                }
                onAction(action)
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
            if (isReady) {
                IconButton(
                    onClick = {
                        if (isPlaying) {
                            player.pause()
                            isPlaying = false
                        } else {
                            runCatching {
                                player.seekTo(positionMs)
                                player.start()
                                isPlaying = true
                            }
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else if (file.status != DownloadStatus.UPLOADING && file.status != DownloadStatus.DOWNLOADING) {
                Icon(
                    imageVector = if (file.status == DownloadStatus.FAILED) Icons.Rounded.Refresh else Icons.Rounded.Download,
                    contentDescription = file.status.name,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            if (file.status == DownloadStatus.DOWNLOADING || file.status == DownloadStatus.UPLOADING) {
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
                    .pointerInput(durationMs, amplitudes) {
                        if (durationMs > 0 && amplitudes != null) {
                            detectHorizontalDragGestures(
                                onDragStart = { isDragging = true },
                                onDragEnd = {
                                    val newPos = (positionMs + dragOffsetMs.toInt())
                                        .coerceIn(0, durationMs.coerceAtLeast(1))
                                    runCatching { player.seekTo(newPos) }
                                    positionMs = newPos
                                    dragOffsetMs = 0f
                                    isDragging = false
                                },
                                onDragCancel = {
                                    dragOffsetMs = 0f
                                    isDragging = false
                                },
                                onHorizontalDrag = { _, dragPx ->
                                    if (durationMs <= 0) return@detectHorizontalDragGestures
                                    val msPerPx = durationMs.toFloat() / size.width
                                    val deltaMs = dragPx / msPerPx
                                    dragOffsetMs = (dragOffsetMs + deltaMs).coerceIn(
                                        minimumValue = -positionMs.toFloat(),
                                        maximumValue = (durationMs - positionMs).toFloat()
                                    )
                                }
                            )
                        }
                    }
            ) {
                Waveform(
                    amplitudes = amplitudes ?: emptyList(),
                    progress = progress,
                    isSeeking = isDragging,
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
    isSeeking: Boolean,
    modifier: Modifier = Modifier
) {
    val activeColor = if (isSeeking) Color(0xFF34C759) else MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    val placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
    
    Canvas(modifier = modifier) {
        if (amplitudes.isEmpty()) {
            val placeholderCount = AmplitudeExtractor.AMPLITUDES_COUNT
            val barWidth = size.width / placeholderCount
            val centerY = size.height / 2f
            val barHeight = (size.height * 0.3f).coerceAtLeast(2f)
            for (i in 0 until placeholderCount) {
                val x = i * barWidth
                drawRoundRect(
                    color = placeholderColor,
                    topLeft = Offset(x + barWidth * 0.25f, centerY - barHeight / 2f),
                    size = Size(barWidth * 0.5f, barHeight),
                    cornerRadius = CornerRadius(barWidth / 4f, barWidth / 4f)
                )
            }
            return@Canvas
        }
        
        val barCount = amplitudes.size
        val barWidth = size.width / barCount
        val centerY = size.height / 2f
        val progressX = size.width * progress.coerceIn(0f, 1f)
        
        amplitudes.forEachIndexed { i, amp ->
            val barHeight = (size.height * amp).coerceAtLeast(2f)
            val x = i * barWidth
            val color = if (x < progressX) activeColor else inactiveColor
            drawRoundRect(
                color = color,
                topLeft = Offset(x + barWidth * 0.25f, centerY - barHeight / 2f),
                size = Size(barWidth * 0.5f, barHeight),
                cornerRadius = CornerRadius(barWidth / 4f, barWidth / 4f)
            )
        }
    }
}
