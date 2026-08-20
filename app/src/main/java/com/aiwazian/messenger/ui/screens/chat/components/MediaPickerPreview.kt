/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.request.ImageRequest
import com.aiwazian.messenger.domain.DeviceMediaItem

/**
 * Предпросмотр галереи во весь экран.
 *
 * Это отдельное окно, а не оверлей: шторка вложений живёт в своём окне, и
 * растянуть внутри неё что-то на весь экран нельзя.
 *
 * Видео проигрывается тем же [VideoPlayerItem], что и в чате, только без
 * скорости и зацикливания: здесь это лишние настройки.
 */
@Composable
fun MediaPickerPreview(
    media: List<DeviceMediaItem>,
    initialIndex: Int,
    selectionNumber: (DeviceMediaItem) -> Int,
    onToggleSelection: (DeviceMediaItem) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val context = LocalContext.current
        
        var isUiVisible by remember { mutableStateOf(true) }
        
        val pagerState = rememberPagerState(
            initialPage = initialIndex.coerceIn(0, (media.size - 1).coerceAtLeast(0)),
            pageCount = { media.size })
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val item = media[page]
                
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (item.isVideo) {
                        VideoPlayerItem(
                            uri = item.uri,
                            isCurrentPage = pagerState.currentPage == page,
                            isUiVisible = isUiVisible,
                            isLooping = false,
                            playbackSpeed = 1f,
                            contentModifier = Modifier,
                            onPlayingChanged = {},
                            onShowUiRequest = { isUiVisible = true })
                    } else {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(item.uri)
                                .decoderFactory(GifDecoder.Factory())
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    isUiVisible = !isUiVisible
                                }
                        )
                    }
                }
            }
            
            AnimatedVisibility(
                visible = isUiVisible,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val currentItem = media.getOrNull(pagerState.currentPage)
                
                TopAppBar(
                    title = {}, navigationIcon = {
                        IconButton(
                            onClick = onDismiss, colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack, null
                            )
                        }
                    }, actions = {
                        if (currentItem != null) {
                            IconButton(onClick = { onToggleSelection(currentItem) }) {
                                MediaSelectionBadge(number = selectionNumber(currentItem))
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    modifier = Modifier.background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                Color.Transparent
                            )
                        )
                    )
                )
            }
        }
    }
}
