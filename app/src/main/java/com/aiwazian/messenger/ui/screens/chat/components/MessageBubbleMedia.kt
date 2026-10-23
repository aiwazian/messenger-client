/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.gif.GifDecoder
import coil3.request.ImageRequest
import coil3.video.VideoFrameDecoder
import coil3.video.videoFrameMillis
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.MessageAttachment
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.enums.FileAction
import com.aiwazian.messenger.extensions.formatFileSize
import com.aiwazian.messenger.extensions.getDuration
import com.aiwazian.messenger.extensions.sharedElement
import com.aiwazian.messenger.ui.components.formatDuration
import com.aiwazian.messenger.ui.components.mediaTransitionOrigin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun MediaPlaceholder(
    attachment: MessageAttachment,
    onFileAction: (MessageAttachment, FileAction) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .clickable { onFileAction(attachment, attachment.status.toFileAction()) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = attachment.size.formatFileSize(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp),
            fontSize = 12.sp,
            lineHeight = 12.sp
        )

        MediaStatusIndicator(attachment.status)
    }
}

@Composable
internal fun MediaThumbnail(
    attachment: MessageAttachment,
    mediaUri: Uri,
    cacheKey: String,
    transitionKey: String,
    onFileAction: (MessageAttachment, FileAction) -> Unit
) {
    val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val concealViewOnce = attachment.viewOnce && !canBlur
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .blur(if (attachment.viewOnce && canBlur) 18.dp else 0.dp)
    ) {
        if (concealViewOnce) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onFileAction(attachment, FileAction.OPEN) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.VisibilityOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (attachment.type == AttachmentType.VIDEO) {
            VideoThumbnail(
                videoUri = mediaUri,
                cacheKey = cacheKey,
                transitionKey = transitionKey
            ) {
                onFileAction(attachment, FileAction.OPEN)
            }
        } else {
            ImageThumbnail(
                imageUri = mediaUri,
                cacheKey = cacheKey,
                transitionKey = transitionKey,
                isGif = attachment.type == AttachmentType.GIF
            ) {
                onFileAction(attachment, FileAction.OPEN)
            }
        }

        if (attachment.status.isTransferring()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f))
                    .clickable { onFileAction(attachment, attachment.status.toFileAction()) },
                contentAlignment = Alignment.Center
            ) {
                MediaStatusIndicator(attachment.status)
            }
        }
    }
}

@Composable
internal fun MediaStatusIndicator(status: DownloadStatus) {
    when (status) {
        DownloadStatus.DOWNLOADING -> {
            CircularWavyProgressIndicator()
            Icon(Icons.Rounded.Pause, null)
        }

        DownloadStatus.UPLOADING -> {
            CircularWavyProgressIndicator()
            Icon(Icons.Rounded.Close, null)
        }

        DownloadStatus.PAUSED -> {
            Icon(Icons.Rounded.Downloading, null)
        }

        else -> {
            Icon(Icons.Rounded.Download, null)
        }
    }
}

@Composable
private fun VideoThumbnail(
    videoUri: Uri,
    cacheKey: String,
    transitionKey: String,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val decoderFactory = remember { VideoFrameDecoder.Factory() }

    val request = remember(context, videoUri, cacheKey, decoderFactory) {
        ImageRequest.Builder(context)
            .data(videoUri)
            .decoderFactory(decoderFactory)
            .videoFrameMillis(0)
            .memoryCacheKey(cacheKey)
            .placeholderMemoryCacheKey(cacheKey)
            .build()
    }

    val duration by produceState(0L, videoUri) {
        value = withContext(Dispatchers.IO) { videoUri.getDuration(context) }
    }

    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .mediaTransitionOrigin(transitionKey)
    ) {
        AsyncImage(
            model = request,
            contentDescription = "Thumbnail of the video",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .sharedElement(key = transitionKey)
                .fillMaxSize()
                .clip(MaterialTheme.shapes.extraSmall)
        )
        if (duration > 0) {
            Box(
                modifier = Modifier
                    .padding(start = 4.dp, top = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f))
            ) {
                Text(
                    text = formatDuration(duration),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(vertical = 2.dp, horizontal = 4.dp),
                    fontSize = 12.sp,
                    lineHeight = 12.sp
                )
            }
        }
    }
}

@Composable
private fun ImageThumbnail(
    imageUri: Uri,
    cacheKey: String,
    transitionKey: String,
    isGif: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val decoderFactory = remember { GifDecoder.Factory() }

    val request = remember(context, imageUri, cacheKey, decoderFactory) {
        ImageRequest.Builder(context)
            .data(imageUri)
            .decoderFactory(decoderFactory)
            .memoryCacheKey(cacheKey)
            .placeholderMemoryCacheKey(cacheKey)
            .build()
    }

    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .mediaTransitionOrigin(transitionKey)
    ) {
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .sharedElement(key = transitionKey)
                .fillMaxSize()
                .clip(MaterialTheme.shapes.extraSmall)
        )
        if (isGif) {
            Box(
                modifier = Modifier
                    .padding(start = 4.dp, top = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f))
            ) {
                Text(
                    text = stringResource(R.string.gif).uppercase(),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(vertical = 2.dp, horizontal = 4.dp),
                    fontSize = 12.sp,
                    lineHeight = 12.sp
                )
            }
        }
    }
}

internal fun DownloadStatus.isTransferring(): Boolean {
    return this == DownloadStatus.UPLOADING ||
            this == DownloadStatus.DOWNLOADING ||
            this == DownloadStatus.PAUSED
}

internal fun DownloadStatus.toFileAction(): FileAction {
    return if (this == DownloadStatus.UPLOADING) FileAction.CANCEL else FileAction.DOWNLOAD
}
