/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.aiwazian.messenger.domain.DeviceMediaItem
import com.aiwazian.messenger.ui.components.animatedBackgroundAlpha
import com.aiwazian.messenger.ui.components.animatedOffsetY
import com.aiwazian.messenger.ui.components.dismissDragGestures
import com.aiwazian.messenger.ui.components.mediaHeroBackground
import com.aiwazian.messenger.ui.components.mediaHeroContainer
import com.aiwazian.messenger.ui.components.mediaHeroContent
import com.aiwazian.messenger.ui.components.pickerMediaKey
import com.aiwazian.messenger.ui.components.rememberDismissDragState
import com.aiwazian.messenger.ui.components.rememberMediaHeroState

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
 */
@Composable
fun MediaPickerPreview(
    media: List<DeviceMediaItem>,
    initialIndex: Int,
    selectionNumber: (DeviceMediaItem) -> Int,
    onToggleSelection: (DeviceMediaItem) -> Unit,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, (media.size - 1).coerceAtLeast(0)),
        pageCount = { media.size })
    
    val dismissDragState = rememberDismissDragState()
    val backgroundAlpha = dismissDragState.animatedBackgroundAlpha()
    
    /*
     * Переход создаётся до окна: его же спрашивает само окно, когда его закрывают
     * кнопкой «name», и ответить надо раньше, чем окно успеет исчезнуть.
     */
    val hero = rememberMediaHeroState(
        originKey = media.getOrNull(pagerState.currentPage)?.let { pickerMediaKey(it.uri) },
        dragOffsetY = dismissDragState.animatedOffsetY(),
        onDismissed = onDismiss
    )
    
    Dialog(
        onDismissRequest = hero::dismiss, properties = DialogProperties(
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
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .mediaHeroBackground(hero, MaterialTheme.colorScheme.surface) { backgroundAlpha }
                .navigationBarsPadding()
                .mediaHeroContainer(hero)
        ) {
            HorizontalPager(
                state = pagerState, modifier = Modifier
                    .fillMaxSize()
                    .dismissDragGestures(
                        state = dismissDragState,
                        onTap = { isUiVisible = !isUiVisible },
                        onDismiss = hero::dismiss
                    )
                    .mediaHeroContent(hero)) { page ->
                val item = media[page]
                
                ZoomableMediaPage(
                    uri = item.uri,
                    isVideo = item.isVideo,
                    isCurrentPage = pagerState.currentPage == page,
                    pagerState = pagerState,
                    onTap = { isUiVisible = !isUiVisible },
                    isVideoUiVisible = !dismissDragState.isDragging && isUiVisible,
                    onShowVideoUiRequest = { isUiVisible = true })
            }
            
            AnimatedVisibility(
                visible = !dismissDragState.isDragging && isUiVisible && hero.isSettled,
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
                            onClick = hero::dismiss, colors = IconButtonDefaults.iconButtonColors(
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
