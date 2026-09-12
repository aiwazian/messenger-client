/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Hd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.ui.compose.state.rememberPlaybackSpeedState
import androidx.media3.ui.compose.state.rememberSeekBackButtonState
import androidx.media3.ui.compose.state.rememberSeekForwardButtonState
import coil3.compose.AsyncImage
import coil3.gif.AnimatedImageDecoder
import coil3.request.ImageRequest
import com.aiwazian.messenger.ui.components.MediaTransformState
import com.aiwazian.messenger.ui.components.PlayerSeekIndicator
import com.aiwazian.messenger.ui.components.mediaTransform
import com.aiwazian.messenger.ui.components.rememberZoomableState
import com.aiwazian.messenger.ui.components.zoomableContent
import com.aiwazian.messenger.ui.components.zoomableGestures
import com.aiwazian.messenger.utils.VibrationManager
import com.aiwazian.messenger.utils.VibrationPattern
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SEEK_ZONE_FRACTION = 1f / 3f
private const val SEEK_INDICATOR_TIMEOUT_MS = 600L

internal const val PLAYER_FAST_FORWARD_SPEED = 2f

@Composable
internal fun ZoomableMediaPage(
    uri: Uri,
    isVideo: Boolean,
    isCurrentPage: Boolean,
    pagerState: PagerState,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    isPageChangeEnabled: Boolean = true,
    isVideoUiVisible: Boolean = false,
    isVideoLooping: Boolean = false,
    videoPlaybackSpeed: Float = 1f,
    isVideoSeekBarVisible: Boolean = true,
    isTransformable: Boolean = false,
    isTransformed: Boolean = false,
    videoQualityIcon: ImageVector = Icons.Outlined.Hd,
    onVideoQualityClick: (() -> Unit)? = null,
    onVideoTransformClick: (() -> Unit)? = null,
    transformState: MediaTransformState? = null,
    onVideoPlayingChanged: (Boolean) -> Unit = {},
    onShowVideoUiRequest: () -> Unit = {},
    onVideoFastForwardChanged: (Boolean) -> Unit = {},
    onHeroContentSizeChanged: (Size) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val zoomableState = rememberZoomableState()
    var contentSize by remember { mutableStateOf(Size.Zero) }
    var pageSize by remember { mutableStateOf(IntSize.Zero) }
    var player by remember { mutableStateOf<Player?>(null) }
    
    val vibrationManager = remember(context) { VibrationManager(context.applicationContext) }
    
    val seekBackButtonState = rememberSeekBackButtonState(player)
    val seekForwardButtonState = rememberSeekForwardButtonState(player)
    val playbackSpeedState = rememberPlaybackSpeedState(player)
    
    var seekAmountMs by remember { mutableStateOf(0L) }
    var isSeekIndicatorVisible by remember { mutableStateOf(false) }
    var isFastForwarding by remember { mutableStateOf(false) }
    val seekIndicatorJob = remember { mutableStateOf<Job?>(null) }
    
    val currentOnVideoFastForwardChanged by rememberUpdatedState(onVideoFastForwardChanged)
    
    fun showSeekIndicator(amountMs: Long) {
        seekAmountMs = amountMs
        isSeekIndicatorVisible = true
        seekIndicatorJob.value?.cancel()
        seekIndicatorJob.value = coroutineScope.launch {
            delay(SEEK_INDICATOR_TIMEOUT_MS)
            isSeekIndicatorVisible = false
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            if (isFastForwarding) {
                currentOnVideoFastForwardChanged(false)
            }
        }
    }
    
    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            zoomableState.reset()
            
            if (isFastForwarding) {
                isFastForwarding = false
                playbackSpeedState.restoreOverriddenSpeed()
                currentOnVideoFastForwardChanged(false)
            }
        }
    }
    
    val isZoomed by remember(zoomableState) { derivedStateOf { zoomableState.isZoomed } }
    
    LaunchedEffect(isCurrentPage, contentSize, isZoomed) {
        if (isCurrentPage) {
            onHeroContentSizeChanged(if (isZoomed) Size.Zero else contentSize)
        }
    }
    
    val transformModifier = if (transformState != null) {
        Modifier.mediaTransform(transformState, contentSize)
    } else {
        Modifier
    }
    
    val onDoubleTap: ((Offset) -> Boolean)? = if (isVideo) {
        { position ->
            val width = pageSize.width.toFloat()
            
            when {
                width <= 0f -> false
                
                position.x < width * SEEK_ZONE_FRACTION -> {
                    if (seekBackButtonState.isEnabled) {
                        showSeekIndicator(-seekBackButtonState.seekBackAmountMs)
                        seekBackButtonState.onClick()
                        true
                    } else {
                        false
                    }
                }
                
                position.x > width * (1f - SEEK_ZONE_FRACTION) -> {
                    if (seekForwardButtonState.isEnabled) {
                        showSeekIndicator(seekForwardButtonState.seekForwardAmountMs)
                        seekForwardButtonState.onClick()
                        true
                    } else {
                        false
                    }
                }
                
                else -> false
            }
        }
    } else {
        null
    }
    
    val onLongPress: (() -> Unit)? = if (isVideo) {
        {
            if (playbackSpeedState.isEnabled) {
                isFastForwarding = true
                playbackSpeedState.temporarilyOverrideSpeedWith(PLAYER_FAST_FORWARD_SPEED)
                vibrationManager.vibrate(VibrationPattern.TactileResponse)
                currentOnVideoFastForwardChanged(true)
            }
        }
    } else {
        null
    }
    
    val onLongPressFinished: (() -> Unit)? = if (isVideo) {
        {
            if (isFastForwarding) {
                isFastForwarding = false
                playbackSpeedState.restoreOverriddenSpeed()
                currentOnVideoFastForwardChanged(false)
            }
        }
    } else {
        null
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size -> pageSize = size }
            .zoomableGestures(
                state = zoomableState,
                onTap = onTap,
                onDoubleTap = onDoubleTap,
                onLongPress = onLongPress,
                onLongPressFinished = onLongPressFinished,
                onPanBeyondEdge = { pan ->
                    if (isPageChangeEnabled) {
                        pagerState.dispatchRawDelta(-pan)
                    }
                },
                onPanBeyondEdgeFinished = {
                    if (isPageChangeEnabled) {
                        coroutineScope.launch { pagerState.settleAfterEdgePan() }
                    }
                }), contentAlignment = Alignment.Center
    ) {
        if (isVideo) {
            VideoPlayerItem(
                uri = uri,
                isCurrentPage = isCurrentPage,
                isUiVisible = isVideoUiVisible,
                isLooping = isVideoLooping,
                playbackSpeed = videoPlaybackSpeed,
                modifier = Modifier
                    .zoomableContent(zoomableState)
                    .then(transformModifier),
                isSeekBarVisible = isVideoSeekBarVisible,
                isTransformable = isTransformable,
                isTransformed = isTransformed,
                qualityIcon = videoQualityIcon,
                onQualityClick = onVideoQualityClick,
                onTransformClick = onVideoTransformClick,
                onPlayingChanged = onVideoPlayingChanged,
                onShowUiRequest = onShowVideoUiRequest,
                onPlayerReady = { readyPlayer -> player = readyPlayer },
                onContentSizeChanged = { size ->
                    contentSize = size
                    zoomableState.updateContentSize(size)
                })
            
            PlayerSeekIndicator(
                seekAmountMs = seekAmountMs,
                visible = isSeekIndicatorVisible,
                modifier = Modifier
                    .align(if (seekAmountMs < 0) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 32.dp)
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(uri)
                    .decoderFactory(AnimatedImageDecoder.Factory())
                    .build(),
                contentDescription = null,
                onSuccess = { success ->
                    contentSize = success.painter.intrinsicSize
                    zoomableState.updateContentSize(success.painter.intrinsicSize)
                },
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .zoomableContent(zoomableState)
                    .then(transformModifier)
            )
        }
    }
}
