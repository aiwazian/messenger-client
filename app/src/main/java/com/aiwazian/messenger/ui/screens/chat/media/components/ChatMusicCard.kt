/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.media.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.aiwazian.messenger.domain.ChatMediaItem
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.extensions.formatFileSize
import com.aiwazian.messenger.extensions.getFileIcon
import com.aiwazian.messenger.extensions.toInstance
import com.aiwazian.messenger.extensions.toPrettyDateWithYear
import com.aiwazian.messenger.ui.components.formatDuration
import com.aiwazian.messenger.utils.AudioMetadataCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ChatMusicCard(
    music: ChatMediaItem,
    metadata: AudioTrackMetadata?,
    isCurrentTrack: Boolean,
    isPlaying: Boolean,
    positionMs: Int,
    durationMs: Int,
    onClick: () -> Unit,
    onSeek: (Int) -> Unit,
    onMetadataResolved: (AudioTrackMetadata?) -> Unit,
    modifier: Modifier = Modifier
) {
    val localUri = music.localUri
    val isDownloaded = localUri != null
    val isDownloading = music.status == DownloadStatus.DOWNLOADING

    val coverBitmap by produceState<ImageBitmap?>(null, metadata?.cover) {
        val bytes = metadata?.cover ?: return@produceState
        value = withContext(Dispatchers.IO) {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }
    }

    LaunchedEffect(localUri, metadata) {
        if (localUri == null || metadata != null) {
            return@LaunchedEffect
        }

        val resolved = AudioMetadataCache.get(
            fileId = music.fileId,
            filePath = localUri.path ?: localUri.toString(),
            fallbackTitle = music.name
        )
        onMetadataResolved(resolved)
    }

    val trackDurationMs = when {
        isCurrentTrack && durationMs > 0 -> durationMs
        else -> metadata?.durationMs ?: music.durationMs ?: 0
    }
    val trackPositionMs = if (isCurrentTrack) positionMs else 0
    var dragPositionMs by remember { mutableStateOf<Int?>(null) }
    val shownPositionMs = dragPositionMs ?: trackPositionMs
    val isDurationKnown = trackDurationMs > 0
    val showPause = isPlaying && isCurrentTrack
    val currentOnSeek by rememberUpdatedState(onSeek)
    val artist = metadata?.artist?.takeIf { it.isNotBlank() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            if (coverBitmap != null) {
                Image(
                    bitmap = coverBitmap!!,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = music.extension.getFileIcon(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            when {
                isDownloading -> {
                    if (music.progress == 0) {
                        CircularWavyProgressIndicator(
                            modifier = Modifier.size(48.dp)
                        )
                    } else {
                        val animatedProgress by animateFloatAsState(
                            targetValue = music.progress / 100f,
                            animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
                        )

                        CircularWavyProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                isDownloaded && coverBitmap != null -> {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (showPause) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                isDownloaded -> {
                    Icon(
                        imageVector = if (showPause) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = metadata?.title ?: music.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 16.sp
            )

            when {
                isDownloading -> {
                    if (music.progress == 0) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    } else {
                        val animatedProgress by animateFloatAsState(
                            targetValue = music.progress / 100f,
                            animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
                        )

                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                isCurrentTrack && isDurationKnown -> {
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

                    Text(
                        text = "${formatDuration(shownPositionMs.toLong())} / ${formatDuration(trackDurationMs.toLong())}",
                        fontSize = 10.sp,
                        lineHeight = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (!isDownloaded) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        val details = listOfNotNull(
                            artist,
                            "${music.size.formatFileSize()}, ${music.sendTime.toInstance().toPrettyDateWithYear()}"
                        )

                        Text(
                            text = details.joinToString(" · "),
                            fontSize = 12.sp,
                            lineHeight = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
