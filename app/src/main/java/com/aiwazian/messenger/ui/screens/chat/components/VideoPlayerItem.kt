/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Hd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import androidx.media3.ui.compose.material3.Player as Media3Player
import com.aiwazian.messenger.ui.components.PlayerBottomControls
import com.aiwazian.messenger.ui.components.PlayerCenterControls

@Composable
fun VideoPlayerItem(
    uri: Uri,
    isCurrentPage: Boolean,
    isUiVisible: Boolean,
    modifier: Modifier = Modifier,
    isLooping: Boolean = false,
    playbackSpeed: Float = 1.0f,
    isSeekBarVisible: Boolean = true,
    isTransformable: Boolean = false,
    isTransformed: Boolean = false,
    qualityIcon: ImageVector = Icons.Outlined.Hd,
    onQualityClick: (() -> Unit)? = null,
    onTransformClick: (() -> Unit)? = null,
    onPlayingChanged: (Boolean) -> Unit = {},
    onShowUiRequest: () -> Unit = {},
    onContentSizeChanged: (Size) -> Unit = {}
) {
    val context = LocalContext.current

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = if (isLooping) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
            playbackParameters = PlaybackParameters(playbackSpeed)
            prepare()
        }
    }

    LaunchedEffect(isLooping) {
        player.repeatMode = if (isLooping) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
    }

    LaunchedEffect(playbackSpeed) {
        player.playbackParameters = PlaybackParameters(playbackSpeed)
    }

    var isBuffering by remember { mutableStateOf(false) }

    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            player.pause()
        }
    }

    val currentIsLooping by rememberUpdatedState(isLooping)
    val currentOnPlayingChanged by rememberUpdatedState(onPlayingChanged)
    val currentOnShowUiRequest by rememberUpdatedState(onShowUiRequest)
    val currentOnContentSizeChanged by rememberUpdatedState(onContentSizeChanged)

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                currentOnPlayingChanged(playing)
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                currentOnContentSizeChanged(videoSize.toContentSize())
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING

                if (playbackState == Player.STATE_ENDED && !currentIsLooping) {
                    currentOnShowUiRequest()
                }
            }
        }
        player.addListener(listener)

        currentOnContentSizeChanged(player.videoSize.toContentSize())

        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Слоты Player оставлены пустыми: зум и поворот приходят в modifier, а он применяется
        // ко всему Player сразу, поэтому контролы рисуем отдельным слоем без трансформаций
        Media3Player(
            player = player,
            modifier = Modifier
                .fillMaxSize()
                .then(modifier),
            surfaceType = if (isTransformable) {
                SURFACE_TYPE_TEXTURE_VIEW
            } else {
                SURFACE_TYPE_SURFACE_VIEW
            },
            keepContentOnReset = true,
            shutter = {},
            topControls = null,
            centerControls = null,
            bottomControls = null,
            errorOverlay = null
        )

        PlayerCenterControls(
            player = player,
            showControls = isUiVisible,
            modifier = Modifier.align(Alignment.Center),
            isBuffering = isBuffering
        )

        PlayerBottomControls(
            player = player,
            showControls = isUiVisible,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            isSeekBarVisible = isSeekBarVisible,
            qualityIcon = qualityIcon,
            isTransformed = isTransformed,
            onQualityClick = onQualityClick,
            onTransformClick = onTransformClick
        )
    }
}

private fun VideoSize.toContentSize(): Size {
    val pixelRatio = if (pixelWidthHeightRatio > 0f) pixelWidthHeightRatio else 1f

    return Size(width * pixelRatio, height.toFloat())
}
