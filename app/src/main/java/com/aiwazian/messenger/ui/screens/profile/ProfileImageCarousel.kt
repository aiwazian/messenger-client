/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.profile

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import androidx.media3.ui.compose.state.rememberPresentationState
import coil3.compose.AsyncImage
import com.aiwazian.messenger.extensions.getFileType
import com.aiwazian.messenger.extensions.sharedBounds
import com.aiwazian.messenger.ui.components.avatarImageRequest

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
                        VideoProfileImage(
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

@OptIn(UnstableApi::class)
@Composable
private fun VideoProfileImage(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(uri) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> player.pause()
                Lifecycle.Event.ON_RESUME -> player.play()
                else -> Unit
            }
        }

        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
            player.release()
        }
    }

    val presentationState = rememberPresentationState(player)

    Box(modifier) {
        PlayerSurface(
            player = player,
            modifier = Modifier.fillMaxSize(),
            surfaceType = SURFACE_TYPE_TEXTURE_VIEW
        )

        if (presentationState.coverSurface) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
        }
    }
}
