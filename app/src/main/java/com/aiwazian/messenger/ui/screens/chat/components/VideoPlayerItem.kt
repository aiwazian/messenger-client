/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Hd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame
import com.aiwazian.messenger.ui.components.PlayerUi
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun VideoPlayerItem(
    uri: Uri,
    isCurrentPage: Boolean,
    isUiVisible: Boolean,
    modifier: Modifier = Modifier,
    isLooping: Boolean = false,
    playbackSpeed: Float = 1.0f,
    isSeekBarVisible: Boolean = true,
    qualityIcon: ImageVector = Icons.Outlined.Hd,
    onQualityClick: (() -> Unit)? = null,
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
    
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isBuffering by remember { mutableStateOf(false) }
    
    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage && isPlaying) {
            player.pause()
        }
    }
    
    val currentIsLooping by rememberUpdatedState(isLooping)
    val currentOnShowUiRequest by rememberUpdatedState(onShowUiRequest)
    val currentOnContentSizeChanged by rememberUpdatedState(onContentSizeChanged)

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                onPlayingChanged(playing)
            }
            
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                currentOnContentSizeChanged(videoSize.toContentSize())
            }
            
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    duration = player.duration.coerceAtLeast(0L)
                }
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
    
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = player.currentPosition.coerceAtLeast(0L)
            delay(16.milliseconds)
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        ContentFrame(
            player = player,
            modifier = Modifier
                .fillMaxSize()
                .then(modifier),
            keepContentOnReset = true,
            shutter = {}
        )
        
        AnimatedVisibility(
            visible = isUiVisible,
            modifier = Modifier.fillMaxSize()
        ) {
            PlayerUi(
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                duration = duration,
                isBuffering = isBuffering,
                onSeekBarPositionChange = { newPos ->
                    currentPosition = newPos
                },
                onSeekBarPositionChangeFinished = {
                    player.seekTo(currentPosition)
                },
                onPlayPauseClick = {
                    if (!isPlaying && player.playbackState == Player.STATE_ENDED) {
                        player.seekTo(0L)
                        player.play()
                    } else if (isPlaying) {
                        player.pause()
                    } else {
                        player.play()
                    }
                },
                isSeekBarVisible = isSeekBarVisible,
                qualityIcon = qualityIcon,
                onQualityClick = onQualityClick
            )
        }
    }
}

private fun VideoSize.toContentSize(): Size {
    val pixelRatio = if (pixelWidthHeightRatio > 0f) pixelWidthHeightRatio else 1f
    
    return Size(width * pixelRatio, height.toFloat())
}
