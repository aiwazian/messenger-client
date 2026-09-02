/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CropRotate
import androidx.compose.material.icons.rounded.Hd
import androidx.compose.material.icons.rounded.Sd
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.DeviceMediaItem
import com.aiwazian.messenger.extensions.formatFileSize
import com.aiwazian.messenger.ui.components.BottomBarScrim
import com.aiwazian.messenger.ui.components.MediaFlipButton
import com.aiwazian.messenger.ui.components.MediaOverlayIconButton
import com.aiwazian.messenger.ui.components.MediaRotateButton
import com.aiwazian.messenger.ui.components.TopBarScrim
import com.aiwazian.messenger.ui.components.animatedBackgroundAlpha
import com.aiwazian.messenger.ui.components.animatedOffsetY
import com.aiwazian.messenger.ui.components.dismissDragGestures
import com.aiwazian.messenger.ui.components.mediaHeroBackground
import com.aiwazian.messenger.ui.components.mediaHeroContainer
import com.aiwazian.messenger.ui.components.mediaHeroContent
import com.aiwazian.messenger.ui.components.pickerMediaKey
import com.aiwazian.messenger.ui.components.rememberDismissDragState
import com.aiwazian.messenger.ui.components.rememberMediaHeroState
import com.aiwazian.messenger.ui.components.rememberMediaTransformState
import com.aiwazian.messenger.utils.media.MediaCompressionConfig
import com.aiwazian.messenger.utils.media.MediaTransform
import com.aiwazian.messenger.utils.media.VideoMetadata
import com.aiwazian.messenger.utils.media.VideoQuality
import com.aiwazian.messenger.utils.media.estimateSizeBytes
import com.aiwazian.messenger.utils.media.frameFor
import kotlinx.coroutines.launch

/**
 * Что правит нижняя панель предпросмотра.
 *
 * Панель одна, поэтому режимы взаимно исключаются: иначе слайдер качества и
 * кнопки поворота боролись бы за одно и то же место.
 */
private enum class PreviewMode {
    Content, Quality, Transform
}

@Composable
fun MediaPickerPreview(
    media: List<DeviceMediaItem>,
    initialIndex: Int,
    selectionNumber: (DeviceMediaItem) -> Int,
    onToggleSelection: (DeviceMediaItem) -> Unit,
    onDismiss: () -> Unit,
    openedVideo: VideoMetadata? = null,
    videoQuality: (DeviceMediaItem) -> VideoQuality? = { null },
    onVideoQualityChange: (DeviceMediaItem, VideoQuality) -> Unit = { _, _ -> },
    mediaTransform: (DeviceMediaItem) -> MediaTransform? = { null },
    onMediaTransformChange: (DeviceMediaItem, MediaTransform) -> Unit = { _, _ -> },
    onCurrentItemChange: (DeviceMediaItem?) -> Unit = {}
) {
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, (media.size - 1).coerceAtLeast(0)),
        pageCount = { media.size })
    
    val coroutineScope = rememberCoroutineScope()
    val dismissDragState = rememberDismissDragState()
    val backgroundAlpha = dismissDragState.animatedBackgroundAlpha()
    
    val currentItem = media.getOrNull(pagerState.currentPage)
    
    var mode by remember { mutableStateOf(PreviewMode.Content) }
    var draftQuality by remember { mutableStateOf<VideoQuality?>(null) }
    
    /*
     * Отменённые правки обязаны уйти с кадра целиком, вместе с недоехавшей
     * анимацией поворота. Проще всего собрать состояние заново, а счётчик — повод
     * для этого. “Готово” его не трогает: сохранённое и так совпадает с экраном.
     */
    var transformAttempt by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(currentItem?.uri) {
        mode = PreviewMode.Content
        draftQuality = null
        onCurrentItemChange(currentItem)
    }
    
    val stops = openedVideo?.let { VideoQuality.availableFor(it.shortSide) }.orEmpty()
    
    val defaultQuality = stops.lastOrNull {
        it.shortSide <= MediaCompressionConfig.VIDEO_DEFAULT_QUALITY.shortSide
    } ?: stops.lastOrNull() ?: MediaCompressionConfig.VIDEO_DEFAULT_QUALITY
    
    val savedQuality = currentItem?.let(videoQuality)?.takeIf { stops.contains(it) }
    val selectedQuality = draftQuality ?: savedQuality ?: defaultQuality
    
    val frame = openedVideo?.let { selectedQuality.frameFor(it.width, it.height) }
    val estimate = openedVideo?.let {
        selectedQuality.estimateSizeBytes(it.durationMs, it.sizeBytes)
    }
    
    val qualityIcon = if (selectedQuality.shortSide >= VideoQuality.P720.shortSide) {
        Icons.Rounded.Hd
    } else {
        Icons.Rounded.Sd
    }
    
    val transformState = rememberMediaTransformState(
        initial = currentItem?.let(mediaTransform) ?: MediaTransform.None,
        key = currentItem?.uri to transformAttempt
    )
    
    /*
     * GIF уезжает на сервер байт в байт, иначе он перестал бы быть анимацией.
     * Значит, применить к нему поворот негде, и обещать его кнопкой нечестно.
     */
    val canTransform = currentItem != null && !currentItem.isGif
    
    val openQuality: (() -> Unit)? = if (stops.size > 1 && mode == PreviewMode.Content) {
        { mode = PreviewMode.Quality }
    } else {
        null
    }
    
    val openTransform: (() -> Unit)? = if (canTransform && mode == PreviewMode.Content) {
        { mode = PreviewMode.Transform }
    } else {
        null
    }
    
    val isEditing = mode != PreviewMode.Content
    
    val hero = rememberMediaHeroState(
        originKey = media.getOrNull(pagerState.currentPage)?.let { pickerMediaKey(it.uri) },
        dragOffsetY = dismissDragState.animatedOffsetY(),
        onDismissed = onDismiss
    )
    
    val goBack: () -> Unit = {
        when (mode) {
            PreviewMode.Quality -> {
                draftQuality = null
                mode = PreviewMode.Content
            }
            
            PreviewMode.Transform -> {
                transformAttempt++
                mode = PreviewMode.Content
            }
            
            PreviewMode.Content -> hero.dismiss()
        }
    }
    
    Dialog(
        onDismissRequest = goBack, properties = DialogProperties(
            usePlatformDefaultWidth = false, decorFitsSystemWindows = false
        )
    ) {
        val view = LocalView.current
        val dialogWindow = (view.parent as? DialogWindowProvider)?.window
        val isLightSurface = MaterialTheme.colorScheme.surface.luminance() > 0.5f
        
        val insetsController = remember(view, dialogWindow) {
            if (dialogWindow == null) {
                return@remember null
            }
            
            dialogWindow.attributes = dialogWindow.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
            
            dialogWindow.setDimAmount(0f)
            
            WindowCompat.getInsetsController(dialogWindow, view).apply {
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        
        LaunchedEffect(insetsController, isLightSurface) {
            val controller = insetsController ?: return@LaunchedEffect
            
            controller.isAppearanceLightStatusBars = isLightSurface
            controller.show(WindowInsetsCompat.Type.statusBars())
        }
        
        DisposableEffect(insetsController) {
            onDispose {
                insetsController?.show(WindowInsetsCompat.Type.statusBars())
            }
        }
        
        val isChromeVisible = !dismissDragState.isDragging && hero.isSettled
        
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .mediaHeroBackground(hero, MaterialTheme.colorScheme.surface) { backgroundAlpha }
                .navigationBarsPadding()
                .mediaHeroContainer(hero),
            topBar = {
                AnimatedVisibility(
                    visible = isChromeVisible,
                    modifier = Modifier.fillMaxWidth(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    TopAppBar(
                        title = {
                            if (frame != null && estimate != null) {
                                AnimatedContent(
                                    targetState = frame,
                                    transitionSpec = {
                                        slideInVertically { -it } + fadeIn() togetherWith slideOutVertically { it } + fadeOut()
                                    }
                                ) { frame ->
                                    Text(
                                        text = "${frame.width} × ${frame.height}, ~${estimate.formatFileSize()}",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }, navigationIcon = {
                            IconButton(
                                onClick = goBack, colors = IconButtonDefaults.iconButtonColors(
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
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = isChromeVisible && isEditing,
                    modifier = Modifier.fillMaxWidth(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (mode) {
                            PreviewMode.Quality -> VideoQualitySlider(
                                stops = stops, selected = selectedQuality,
                                onSelect = { draftQuality = it })
                            
                            PreviewMode.Transform -> Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                MediaFlipButton(state = transformState)
                                
                                MediaRotateButton(state = transformState)
                            }
                            
                            PreviewMode.Content -> Unit
                        }
                        
                        /*
                         * “Сбросить” стоит ровно по центру панели, а не по середине
                         * промежутка между соседями: его появление не должно сдвигать
                         * “Отмена” и “Готово”.
                         */
                        Box(modifier = Modifier.fillMaxWidth()) {
                            TextButton(
                                onClick = goBack, modifier = Modifier.align(Alignment.CenterStart),
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text(text = stringResource(R.string.cancel).uppercase())
                            }
                            
                            AnimatedVisibility(
                                visible = mode == PreviewMode.Transform && transformState.isChanged,
                                modifier = Modifier.align(Alignment.Center),
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                TextButton(
                                    onClick = {
                                        coroutineScope.launch { transformState.reset() }
                                    }, colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Text(
                                        text = stringResource(R.string.media_transform_reset)
                                            .uppercase()
                                    )
                                }
                            }
                            
                            TextButton(
                                onClick = {
                                    if (currentItem != null) {
                                        if (mode == PreviewMode.Transform) {
                                            onMediaTransformChange(
                                                currentItem, transformState.transform
                                            )
                                        } else {
                                            onVideoQualityChange(currentItem, selectedQuality)
                                        }
                                    }
                                    
                                    draftQuality = null
                                    mode = PreviewMode.Content
                                }, modifier = Modifier.align(Alignment.CenterEnd)
                            ) {
                                Text(text = stringResource(R.string.done).uppercase())
                            }
                        }
                    }
                }
            },
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets()
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState, userScrollEnabled = !isEditing, modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (isEditing) {
                                Modifier
                            } else {
                                Modifier.dismissDragGestures(
                                    state = dismissDragState, onDismiss = hero::dismiss
                                )
                            }
                        )
                        .mediaHeroContent(hero)
                ) { page ->
                    val item = media[page]
                    val isCurrentPage = pagerState.currentPage == page
                    
                    Box {
                        ZoomableMediaPage(
                            uri = item.uri,
                            isVideo = item.isVideo,
                            isCurrentPage = isCurrentPage,
                            pagerState = pagerState,
                            onTap = {},
                            isVideoUiVisible = isChromeVisible,
                            isVideoSeekBarVisible = !isEditing,
                            videoQualityIcon = qualityIcon,
                            onVideoQualityClick = if (isCurrentPage) openQuality else null,
                            onVideoTransformClick = if (isCurrentPage) openTransform else null,
                            transformState = if (isCurrentPage) transformState else null,
                            onHeroContentSizeChanged = hero::updateContentSize
                        )
                        
                        TopBarScrim(height = innerPadding.calculateTopPadding())
                        
                        BottomBarScrim(height = innerPadding.calculateBottomPadding())
                    }
                }
                
                /*
                 * У фотографии нет плеера, в который встроена такая же кнопка,
                 * поэтому её пришлось положить на то же место самому предпросмотру.
                 */
                AnimatedVisibility(
                    visible = isChromeVisible && openTransform != null && currentItem?.isVideo == false,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    MediaOverlayIconButton(
                        icon = Icons.Rounded.CropRotate,
                        onClick = { mode = PreviewMode.Transform })
                }
            }
        }
    }
}
