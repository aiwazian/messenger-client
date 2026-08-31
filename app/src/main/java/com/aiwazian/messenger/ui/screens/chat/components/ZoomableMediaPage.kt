/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Hd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.aiwazian.messenger.ui.components.rememberZoomableState
import com.aiwazian.messenger.ui.components.zoomableContent
import com.aiwazian.messenger.ui.components.zoomableGestures
import kotlinx.coroutines.launch

@Composable
internal fun ZoomableMediaPage(
    uri: Uri,
    isVideo: Boolean,
    isCurrentPage: Boolean,
    pagerState: PagerState,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    isVideoUiVisible: Boolean = false,
    isVideoLooping: Boolean = false,
    videoPlaybackSpeed: Float = 1f,
    isVideoSeekBarVisible: Boolean = true,
    videoQualityIcon: ImageVector = Icons.Outlined.Hd,
    onVideoQualityClick: (() -> Unit)? = null,
    onVideoPlayingChanged: (Boolean) -> Unit = {},
    onShowVideoUiRequest: () -> Unit = {},
    onHeroContentSizeChanged: (Size) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val zoomableState = rememberZoomableState()
    var contentSize by remember { mutableStateOf(Size.Zero) }
    
    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            zoomableState.reset()
        }
    }
    
    val isZoomed by remember(zoomableState) { derivedStateOf { zoomableState.isZoomed } }
    
    LaunchedEffect(isCurrentPage, contentSize, isZoomed) {
        if (isCurrentPage) {
            onHeroContentSizeChanged(if (isZoomed) Size.Zero else contentSize)
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .zoomableGestures(
                state = zoomableState,
                onTap = onTap,
                onPanBeyondEdge = { pan -> pagerState.dispatchRawDelta(-pan) },
                onPanBeyondEdgeFinished = {
                    coroutineScope.launch { pagerState.settleAfterEdgePan() }
                }), contentAlignment = Alignment.Center
    ) {
        if (isVideo) {
            VideoPlayerItem(
                uri = uri,
                isCurrentPage = isCurrentPage,
                isUiVisible = isVideoUiVisible,
                isLooping = isVideoLooping,
                playbackSpeed = videoPlaybackSpeed,
                modifier = Modifier.zoomableContent(zoomableState),
                isSeekBarVisible = isVideoSeekBarVisible,
                qualityIcon = videoQualityIcon,
                onQualityClick = onVideoQualityClick,
                onPlayingChanged = onVideoPlayingChanged,
                onShowUiRequest = onShowVideoUiRequest,
                onContentSizeChanged = { size ->
                    contentSize = size
                    zoomableState.updateContentSize(size)
                })
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(uri)
                    .decoderFactory(ImageDecoderDecoder.Factory())
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
            )
        }
    }
}
