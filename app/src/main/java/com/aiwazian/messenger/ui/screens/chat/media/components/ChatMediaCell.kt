/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.media.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.aiwazian.messenger.domain.ChatMediaItem
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.ui.components.chatMediaKey
import com.aiwazian.messenger.ui.components.mediaTransitionOrigin

/**
 * Ячейка сетки медиа.
 *
 * Квадратная и с Crop внутри: именно в этот квадрат полный экран и садится
 * обратно при сворачивании вниз.
 *
 * Переход регистрируется тем же ключом, что и миниатюры в переписке: адресом
 * самого файла. Оба экрана одновременно не видны, так что FullScreenViewer
 * годится без правок.
 */
@Composable
fun ChatMediaCell(
    item: ChatMediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val localUri = item.localUri
    
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (localUri == null) {
            /* Скачивание начинается само, поэтому здесь только его состояние. */
            if (item.status == DownloadStatus.DOWNLOADING && item.progress > 0) {
                val animatedProgress by animateFloatAsState(
                    targetValue = item.progress / 100f,
                    animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
                )
                
                CircularWavyProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(32.dp)
                )
            } else {
                CircularWavyProgressIndicator(modifier = Modifier.size(32.dp))
            }
            
            return@Box
        }
        
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(localUri)
                .apply {
                    when (item.type) {
                        AttachmentType.VIDEO -> {
                            decoderFactory(VideoFrameDecoder.Factory())
                            videoFrameMillis(0)
                        }
                        
                        AttachmentType.GIF -> decoderFactory(GifDecoder.Factory())
                        
                        else -> {}
                    }
                }
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .mediaTransitionOrigin(chatMediaKey(localUri))
        )
        
        if (item.type == AttachmentType.VIDEO) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f))
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
