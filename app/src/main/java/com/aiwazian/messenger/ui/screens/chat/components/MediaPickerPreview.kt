/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.outlined.Hd
import androidx.compose.material.icons.outlined.Sd
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.aiwazian.messenger.utils.media.MediaCompressionConfig
import com.aiwazian.messenger.utils.media.VideoMetadata
import com.aiwazian.messenger.utils.media.VideoQuality
import com.aiwazian.messenger.utils.media.estimateSizeBytes
import com.aiwazian.messenger.utils.media.frameFor

/**
 * Предпросмотр галереи во весь экран.
 *
 * Это отдельное окно, а не оверлей: шторка вложений живёт в своём окне, и
 * растянуть внутри неё что-то на весь экран нельзя. По той же причине переход
 * из миниатюры считается по экранным границам: штатный shared element рисует
 * оверлей только внутри своего окна и через границу окон не работает.
 *
 * Своё окно по умолчанию укладывается между системными панелями, поэтому его просят
 * этого не делать: иначе картинка обрывалась бы под панелью уведомлений, а не
 * заходила за неё, как в [FullScreenViewer].
 *
 * Вертикальный свайп закрывает предпросмотр так же, как в чате, но только пока
 * медиа в исходном размере: увеличенное забирает свайп себе и только чуть-чуть
 * сдвигается. Пока палец ведёт медиа, фон тает, поэтому окно просят не затемнять
 * то, что под ним: иначе за фоном была бы чернота, а не шторка вложений.
 *
 * Видео проигрывается тем же [VideoPlayerItem], что и в чате, только без
 * скорости и зацикливания: здесь это лишние настройки.
 *
 * Панели сверху и снизу отданы Scaffold: он меряет их сам, и по этим высотам
 * рисуются затемнения из Scrims.kt. Медиа при этом уходит под панели целиком.
 *
 * У видео снизу есть настройка сжатия: на её кнопке Sd или Hd — по иконке видно,
 * мелкая ступень выбрана или крупная. Пока настройка открыта, листалка и свайп
 * вниз отключены: слайдер ступеней ведут пальцем по горизонтали, и любой промах
 * уводил бы на соседнее медиа или вовсе закрывал окно.
 *
 * @param openedVideo размеры открытого видео. Без них ступеней нет: не из чего
 * считать ни доступные разрешения, ни примерный вес.
 * @param videoQuality ранее сохранённая ступень: настройка открывается на ней.
 * @param onCurrentItemChange открытое медиа сменилось, в том числе на первом кадре.
 */
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
    onCurrentItemChange: (DeviceMediaItem?) -> Unit = {}
) {
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, (media.size - 1).coerceAtLeast(0)),
        pageCount = { media.size })
    
    val dismissDragState = rememberDismissDragState()
    val backgroundAlpha = dismissDragState.animatedBackgroundAlpha()
    
    val currentItem = media.getOrNull(pagerState.currentPage)
    
    /*
     * Настройка сжатия хранится рядом с окном, а не внутри него: кнопку «назад»
     * спрашивает само окно, и выйти из настройки надо раньше, чем оно закроется.
     */
    var isQualityMode by remember { mutableStateOf(false) }
    var draftQuality by remember { mutableStateOf<VideoQuality?>(null) }
    
    /* Пролистали на другое медиа — настройка закрывается, черновик пуст. */
    LaunchedEffect(currentItem?.uri) {
        isQualityMode = false
        draftQuality = null
        onCurrentItemChange(currentItem)
    }
    
    val stops = openedVideo?.let { VideoQuality.availableFor(it.shortSide) }.orEmpty()
    
    /*
     * Ступень по умолчанию — 720p, но не выше исходника: у 480p-видео
     * предвыбранной окажется сама 480p. У видео мельче 360p ступеней нет вовсе, и
     * остаётся ступень из настроек: кадр по ней всё равно не растянется, зато есть
     * чем считать вес для заголовка.
     */
    val defaultQuality = stops.lastOrNull {
        it.shortSide <= MediaCompressionConfig.VIDEO_DEFAULT_QUALITY.shortSide
    } ?: stops.lastOrNull() ?: MediaCompressionConfig.VIDEO_DEFAULT_QUALITY
    
    /* Сохранённая ступень могла остаться от видео, которое было крупнее этого. */
    val savedQuality = currentItem?.let(videoQuality)?.takeIf { stops.contains(it) }
    val selectedQuality = draftQuality ?: savedQuality ?: defaultQuality
    
    val frame = openedVideo?.let { selectedQuality.frameFor(it.width, it.height) }
    val estimate = openedVideo?.let {
        selectedQuality.estimateSizeBytes(it.durationMs, it.sizeBytes)
    }
    
    /* Мелкая ступень — Sd, 720p и выше — Hd: качество видно, не открывая настройку. */
    val qualityIcon = if (selectedQuality.shortSide >= VideoQuality.P720.shortSide) {
        Icons.Outlined.Hd
    } else {
        Icons.Outlined.Sd
    }
    
    /* Меньше двух ступеней — выбирать не из чего, и кнопки настройки нет. */
    val openQuality: (() -> Unit)? = if (stops.size > 1 && !isQualityMode) {
        { isQualityMode = true }
    } else {
        null
    }
    
    /*
     * Переход создаётся до окна: его же спрашивает само окно, когда его закрывают
     * кнопкой «назад», и ответить надо раньше, чем окно успеет исчезнуть.
     */
    val hero = rememberMediaHeroState(
        originKey = media.getOrNull(pagerState.currentPage)?.let { pickerMediaKey(it.uri) },
        dragOffsetY = dismissDragState.animatedOffsetY(),
        onDismissed = onDismiss
    )
    
    /* «Назад» из настройки сжатия возвращает к полосе воспроизведения, не сохраняя. */
    val goBack: () -> Unit = {
        if (isQualityMode) {
            draftQuality = null
            isQualityMode = false
        } else {
            hero.dismiss()
        }
    }
    
    Dialog(
        onDismissRequest = goBack, properties = DialogProperties(
            usePlatformDefaultWidth = false, decorFitsSystemWindows = false
        )
    ) {
        var isUiVisible by remember { mutableStateOf(true) }
        
        val view = LocalView.current
        val dialogWindow = (view.parent as? DialogWindowProvider)?.window
        val isLightSurface = MaterialTheme.colorScheme.surface.luminance() > 0.5f
        
        val insetsController = remember(view, dialogWindow) {
            if (dialogWindow == null) {
                return@remember null
            }
            
            /* Само окно диалога под вырез экрана не зайдёт. */
            dialogWindow.attributes = dialogWindow.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
            
            /* Затемнение под окном сделало бы фон свайпа чёрным, а не цветом темы. */
            dialogWindow.setDimAmount(0f)
            
            WindowCompat.getInsetsController(dialogWindow, view).apply {
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        
        LaunchedEffect(insetsController, isUiVisible, isLightSurface) {
            val controller = insetsController ?: return@LaunchedEffect
            
            controller.isAppearanceLightStatusBars = isLightSurface
            
            if (isUiVisible) {
                controller.show(WindowInsetsCompat.Type.statusBars())
            } else {
                controller.hide(WindowInsetsCompat.Type.statusBars())
            }
        }
        
        DisposableEffect(insetsController) {
            onDispose {
                insetsController?.show(WindowInsetsCompat.Type.statusBars())
            }
        }
        
        val isChromeVisible = !dismissDragState.isDragging && isUiVisible && hero.isSettled
        
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
                            /*
                             * Разрешение после сжатия и примерный вес: видно и до того,
                             * как настройку открыли. У фотографии заголовка нет.
                             */
                            if (frame != null && estimate != null) {
                                Text(
                                    text = "${frame.width} × ${frame.height}, ~${estimate.formatFileSize()}",
                                    style = MaterialTheme.typography.titleMedium
                                )
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
                /* Слайдер ступеней встаёт на место полосы воспроизведения. */
                AnimatedVisibility(
                    visible = isChromeVisible && isQualityMode,
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
                        VideoQualitySlider(
                            stops = stops, selected = selectedQuality,
                            onSelect = { draftQuality = it })
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = goBack) {
                                Text(text = stringResource(R.string.cancel))
                            }
                            
                            TextButton(
                                onClick = {
                                    val item = currentItem
                                    
                                    if (item != null) {
                                        onVideoQualityChange(item, selectedQuality)
                                    }
                                    
                                    draftQuality = null
                                    isQualityMode = false
                                }) {
                                Text(text = stringResource(R.string.done))
                            }
                        }
                    }
                }
            },
            /* Фон рисует переход из миниатюры: свой у Scaffold только перекрыл бы его. */
            containerColor = Color.Transparent,
            /* Медиа уходит под панели целиком, а свой отступ каждая панель считает сама. */
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState, userScrollEnabled = !isQualityMode, modifier = Modifier
                        .fillMaxSize()
                        .then(
                            /* В настройке сжатия свайп вниз не закрывает окно. */
                            if (isQualityMode) {
                                Modifier
                            } else {
                                Modifier.dismissDragGestures(
                                    state = dismissDragState,
                                    onTap = { isUiVisible = !isUiVisible },
                                    onDismiss = hero::dismiss
                                )
                            }
                        )
                        .mediaHeroContent(hero)) { page ->
                    val item = media[page]
                    val isCurrentPage = pagerState.currentPage == page
                    
                    ZoomableMediaPage(
                        uri = item.uri,
                        isVideo = item.isVideo,
                        isCurrentPage = isCurrentPage,
                        pagerState = pagerState,
                        onTap = { isUiVisible = !isUiVisible },
                        isVideoUiVisible = isChromeVisible,
                        isVideoSeekBarVisible = !isQualityMode,
                        videoQualityIcon = qualityIcon,
                        /* Размеры прочитаны только у открытого видео, у соседних их нет. */
                        onVideoQualityClick = if (isCurrentPage) openQuality else null,
                        onShowVideoUiRequest = { isUiVisible = true },
                        onHeroContentSizeChanged = hero::updateContentSize)
                }
                
                /*
                 * Затемнения гаснут вместе со своими панелями: их высоты Scaffold отдаёт
                 * рывком, как только панель исчезла, и без этого градиент пропадал бы
                 * первым. Поэтому условия видимости у них те же, что у панелей.
                 */
                AnimatedVisibility(
                    visible = isChromeVisible,
                    modifier = Modifier.fillMaxSize(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        TopBarScrim(height = innerPadding.calculateTopPadding())
                    }
                }
                
                AnimatedVisibility(
                    visible = isChromeVisible && isQualityMode,
                    modifier = Modifier.fillMaxSize(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        BottomBarScrim(height = innerPadding.calculateBottomPadding())
                    }
                }
            }
        }
    }
}
