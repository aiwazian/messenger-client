/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.aiwazian.messenger.ui.components.rememberZoomableState
import com.aiwazian.messenger.ui.components.zoomableContent
import com.aiwazian.messenger.ui.components.zoomableGestures
import kotlinx.coroutines.launch

/**
 * Одна страница просмотрщика: фото или видео, которые увеличиваются двойным
 * нажатием и щипком.
 *
 * Страница сама сообщает состоянию зума размер содержимого. Без этого широкую
 * фотографию можно было бы утащить в пустые поля сверху и снизу, а высокую —
 * нельзя было бы пролистать до нижнего края: состояние считало бы, что
 * содержимое занимает весь экран.
 *
 * Просмотрщик в чате и предпросмотр в шторке вложений используют одну и ту же
 * страницу, поэтому зум у них ведёт себя одинаково.
 *
 * GIF во весь экран проигрывается сам: декодер отдаёт анимированный рисунок, а
 * Coil запускает его при появлении. Взят ImageDecoderDecoder, а не GifDecoder:
 * он же умеет анимированные WebP и HEIF. В сетке шторки вместо этого стоит
 * кадр из MediaStore, поэтому лента там не шевелится.
 *
 * @param isCurrentPage открыта ли страница сейчас: у соседних зум сбрасывается.
 * @param pagerState листалка, которой отдаются горизонтальные свайпы увеличенного
 * содержимого, доехавшего до своего края.
 * @param onTap нажатие, которое не является частью жеста зума.
 * @param onHeroContentSizeChanged собственные пропорции показанного: по ним переход
 * обрезает кадр при уменьшении в миниатюру. Нулевой размер означает «считай по
 * всему просмотрщику».
 */
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
    
    /*
     * Увеличенное содержимое выходит за свою рамку, и обрезать по ней значило бы
     * срезать полкадра в первый же момент закрытия. Про зум сообщается как про
     * неизвестный размер: переход возвращается к счёту по всему просмотрщику.
     *
     * Сообщает только открытая страница: соседние затёрли бы чужой размер.
     */
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
                contentModifier = Modifier.zoomableContent(zoomableState),
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
