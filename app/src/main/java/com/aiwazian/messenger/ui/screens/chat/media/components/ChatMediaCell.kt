/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.media.components

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.gif.GifDecoder
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder
import coil3.video.videoFrameMillis
import com.aiwazian.messenger.domain.ChatMediaItem
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.ui.components.chatMediaKey
import com.aiwazian.messenger.ui.components.mediaTransitionOrigin

private const val SELECTED_SCALE = 0.9f
private const val SELECTED_OVERSHOOT_SCALE = 0.85f
private val SELECTED_CORNER_RADIUS = 12.dp

@Composable
fun ChatMediaCell(
    item: ChatMediaItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val localUri = item.localUri
    val fastSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    val settleSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()

    val scale = remember { Animatable(if (isSelected) SELECTED_SCALE else 1f) }

    LaunchedEffect(isSelected) {
        val target = if (isSelected) SELECTED_SCALE else 1f

        if (scale.value == target) return@LaunchedEffect

        if (isSelected) {
            scale.animateTo(SELECTED_OVERSHOOT_SCALE, fastSpec)
            scale.animateTo(target, settleSpec)
        } else {
            scale.animateTo(target, fastSpec)
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value

                    val rounding = ((1f - scale.value) / (1f - SELECTED_SCALE)).coerceIn(0f, 1f)

                    shape = RoundedCornerShape(SELECTED_CORNER_RADIUS * rounding)
                    clip = true
                }
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            if (localUri == null) {
                return@Box
            }

            val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

            if (item.spoiler && !canBlur) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.VisibilityOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
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
                        .blur(if (item.spoiler) 18.dp else 0.dp)
                        .mediaTransitionOrigin(chatMediaKey(item.messageId, localUri))
                )
            }

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

        SelectionCheckBadge(
            visible = isSelected,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}
