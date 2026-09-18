/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import android.graphics.BitmapFactory
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiwazian.messenger.domain.AudioTrackMetadata
import com.aiwazian.messenger.domain.MessageAttachment
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.enums.FileAction
import com.aiwazian.messenger.extensions.formatFileSize
import com.aiwazian.messenger.extensions.getFileIcon
import com.aiwazian.messenger.extensions.isAudioFile
import com.aiwazian.messenger.ui.components.formatDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MessageFile(
    file: MessageAttachment,
    metadata: AudioTrackMetadata? = null,
    isCurrentTrack: Boolean = false,
    isPlaying: Boolean = false,
    positionMs: Int = 0,
    durationMs: Int = 0,
    onAction: (FileAction) -> Unit,
    onSeek: (Int) -> Unit = {}
) {
    val isMusic = file.extension.isAudioFile()
    val coverBitmap by produceState<ImageBitmap?>(null, metadata?.cover) {
        val bytes = metadata?.cover ?: return@produceState
        value = withContext(Dispatchers.IO) {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }
    }

    val trackDurationMs = when {
        isCurrentTrack && durationMs > 0 -> durationMs
        else -> metadata?.durationMs ?: 0
    }
    val trackPositionMs = if (isCurrentTrack) positionMs else 0
    var dragPositionMs by remember { mutableStateOf<Int?>(null) }
    val shownPositionMs = dragPositionMs ?: trackPositionMs
    val isDurationKnown = isMusic && trackDurationMs > 0

    val currentOnSeek by rememberUpdatedState(onSeek)

    val hasCover = isMusic && coverBitmap != null

    Row(
        modifier = Modifier
            .then(if (isMusic) Modifier.width(280.dp) else Modifier)
            .clickable {
                if (file.localUri != null) {
                    onAction(if (isMusic) FileAction.PLAY else FileAction.OPEN)
                    return@clickable
                }

                val action = when (file.status) {
                    DownloadStatus.DOWNLOADING -> FileAction.PAUSE
                    DownloadStatus.PAUSED -> if (isMusic) FileAction.RESUME else FileAction.DOWNLOAD
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
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            if (hasCover) {
                Image(
                    bitmap = coverBitmap!!,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                if (file.localUri == null && file.status != DownloadStatus.UPLOADING && file.status != DownloadStatus.DOWNLOADING) {
                    Icon(
                        imageVector = if (isMusic) Icons.Rounded.MusicNote else Icons.Rounded.Download,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    StatusIcon(file)
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

            if (isMusic && file.localUri != null &&
                file.status != DownloadStatus.UPLOADING &&
                file.status != DownloadStatus.DOWNLOADING
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (isMusic) metadata?.title ?: file.name else file.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 14.sp
            )

            if (isMusic && isCurrentTrack && trackDurationMs > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .pointerInput(trackDurationMs) {
                            detectHorizontalDragGestures(
                                onDragStart = { offset ->
                                    dragPositionMs = offset.x
                                        .coerceIn(0f, size.width.toFloat())
                                        .div(size.width)
                                        .times(trackDurationMs)
                                        .toInt()
                                        .coerceIn(0, trackDurationMs)
                                },
                                onDragEnd = {
                                    dragPositionMs?.let(currentOnSeek)
                                    dragPositionMs = null
                                },
                                onDragCancel = {
                                    dragPositionMs = null
                                },
                                onHorizontalDrag = { change, _ ->
                                    change.consume()
                                    dragPositionMs = change.position.x
                                        .coerceIn(0f, size.width.toFloat())
                                        .div(size.width)
                                        .times(trackDurationMs)
                                        .toInt()
                                        .coerceIn(0, trackDurationMs)
                                }
                            )
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    LinearProgressIndicator(
                        progress = {
                            (shownPositionMs.toFloat() / trackDurationMs).coerceIn(0f, 1f)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Text(
                    text = metadata?.artist
                        ?: "${file.size.formatFileSize()} • ${file.extension.uppercase()}",
                    fontSize = 10.sp,
                    lineHeight = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isDurationKnown) {
                Text(
                    text = "${formatDuration(shownPositionMs.toLong())} / ${formatDuration(trackDurationMs.toLong())}",
                    fontSize = 10.sp,
                    lineHeight = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusIcon(
    file: MessageAttachment
) {
    val icon = when (file.status) {
        DownloadStatus.IDLE -> Icons.Rounded.Download
        DownloadStatus.DOWNLOADING -> Icons.Rounded.Pause
        DownloadStatus.UPLOADING -> Icons.Rounded.Upload
        DownloadStatus.PAUSED -> Icons.Rounded.Downloading
        DownloadStatus.COMPLETED -> file.extension.getFileIcon()
        DownloadStatus.FAILED -> Icons.Rounded.Refresh
        DownloadStatus.CANCELLED -> Icons.Rounded.Download
        DownloadStatus.UPLOADED -> Icons.Rounded.Download
    }
    Icon(
        imageVector = icon,
        contentDescription = file.status.name,
        tint = MaterialTheme.colorScheme.primary
    )
}
