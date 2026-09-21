/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.profile

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.carousel.CarouselDefaults
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import coil3.compose.AsyncImage
import com.aiwazian.messenger.ui.components.avatarImageRequest
import com.aiwazian.messenger.extensions.getFileType
import com.aiwazian.messenger.extensions.sharedBounds
import com.aiwazian.messenger.ui.components.LoopingVideoPlayer

@OptIn(UnstableApi::class)
@Composable
fun ProfileImageCarousel(
    modifier: Modifier = Modifier,
    profileId: Long,
    avatars: List<Uri?>
) {
    val carouselState = rememberCarouselState { avatars.size }

    BoxWithConstraints {
        val itemWidth = if (maxWidth < 500.dp) maxWidth else 300.dp

        HorizontalUncontainedCarousel(
            state = carouselState,
            itemWidth = itemWidth,
            modifier = modifier,
            flingBehavior = if (maxWidth < 500.dp) {
                CarouselDefaults.singleAdvanceFlingBehavior(carouselState)
            } else {
                CarouselDefaults.multiBrowseFlingBehavior(carouselState)
            }
        ) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                val uri = avatars[index]
                if (uri == null) {
                    CircularWavyProgressIndicator()
                } else {
                    val context = LocalContext.current
                    val isVideo = remember(uri) {
                        uri.getFileType(context).startsWith("video/")
                    }

                    val itemModifier = if (index == 0) {
                        Modifier.sharedBounds(key = "chat-avatar-$profileId")
                    } else {
                        Modifier
                    }

                    if (isVideo) {
                        LoopingVideoPlayer(
                            uri = uri,
                            modifier = itemModifier.then(Modifier.fillMaxSize())
                        )
                    } else {
                        AsyncImage(
                            model = avatarImageRequest(context, uri),
                            contentDescription = null,
                            modifier = itemModifier.then(Modifier.fillMaxSize()),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }
        }
    }
}
