/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.animations.expressiveScaleIn
import com.aiwazian.messenger.ui.animations.expressiveScaleOut
import com.aiwazian.messenger.ui.app.AppBottomSheet
import com.aiwazian.messenger.ui.app.AppDropdownMenu
import com.aiwazian.messenger.ui.app.AppDropdownMenuItem
import com.aiwazian.messenger.ui.components.TopBarScrim
import com.aiwazian.messenger.ui.components.animatedBackgroundAlpha
import com.aiwazian.messenger.ui.components.animatedOffsetY
import com.aiwazian.messenger.ui.components.chatMediaKey
import com.aiwazian.messenger.ui.components.dismissDragGestures
import com.aiwazian.messenger.ui.components.mediaHeroBackground
import com.aiwazian.messenger.ui.components.mediaHeroContainer
import com.aiwazian.messenger.ui.components.mediaHeroContent
import com.aiwazian.messenger.ui.components.rememberDismissDragState
import com.aiwazian.messenger.ui.components.rememberMediaHeroState
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

data class ViewerMediaItem(
    val uri: Uri,
    val isVideo: Boolean
)

@Composable
fun FullScreenViewer(
    media: List<ViewerMediaItem>,
    initialPage: Int,
    isVideoLooping: Boolean,
    videoPlaybackSpeed: Float = 1.0f,
    canDownloadMedia: Boolean,
    onVideoLoopingChange: (Boolean) -> Unit,
    onVideoPlaybackSpeedChange: (Float) -> Unit = {},
    onSaveToGallery: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    var isUiVisible by remember { mutableStateOf(true) }
    var showVideoSettings by remember { mutableStateOf(false) }
    var showSpeedBottomSheet by remember { mutableStateOf(false) }
    var showMoreActions by remember { mutableStateOf(false) }
    var isVideoPlaying by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    val dismissDragState = rememberDismissDragState()
    val backgroundAlpha = dismissDragState.animatedBackgroundAlpha()
    
    val view = LocalView.current
    val window = remember { (view.context as Activity).window }
    val insetsController = remember {
        WindowCompat.getInsetsController(window, view).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
    
    LaunchedEffect(isUiVisible) {
        if (isUiVisible) insetsController.show(WindowInsetsCompat.Type.statusBars())
        else insetsController.hide(WindowInsetsCompat.Type.statusBars())
    }
    
    DisposableEffect(Unit) {
        onDispose {
            insetsController.show(WindowInsetsCompat.Type.statusBars())
        }
    }
    
    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, (media.size - 1).coerceAtLeast(0)),
        pageCount = { media.size })
    
    /*
     * Переход привязан к миниатюре той страницы, которую сейчас смотрят, а не к той,
     * с которой открыли: иначе после листания медиа улетало бы в чужой пузырёк.
     * Если такой миниатюры на экране нет, переход сам уведёт медиа за край экрана.
     */
    val hero = rememberMediaHeroState(
        originKey = media.getOrNull(pagerState.currentPage)?.let { chatMediaKey(it.uri) },
        dragOffsetY = dismissDragState.animatedOffsetY(),
        onDismissed = onDismiss
    )
    
    /* Чат закрывал просмотрщик мгновенно, поэтому кнопку назад берёт на себя переход. */
    BackHandler { hero.dismiss() }
    
    LaunchedEffect(pagerState.currentPage) {
        isVideoPlaying = false
    }
    
    LaunchedEffect(
        isUiVisible, isVideoPlaying, lastInteractionTime, showVideoSettings, showMoreActions
    ) {
        if (isUiVisible && isVideoPlaying && !showVideoSettings && !showMoreActions) {
            delay(2000.milliseconds)
            isUiVisible = false
        }
    }
    
    val isChromeVisible = !dismissDragState.isDragging && isUiVisible && hero.isSettled
    
    /*
     * Панель сверху отдана Scaffold: он меряет её сам, и по этой высоте рисуется
     * затемнение из Scrims.kt. Раньше градиент висел модификатором на самой
     * панели, и её размер приходилось повторять за ней руками.
     */
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .mediaHeroBackground(hero, MaterialTheme.colorScheme.surface) { backgroundAlpha }
            .navigationBarsPadding()
            .mediaHeroContainer(hero)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        lastInteractionTime = System.currentTimeMillis()
                    }
                }
            },
        topBar = {
            AnimatedVisibility(
                visible = isChromeVisible,
                modifier = Modifier.fillMaxWidth(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val currentItem = media.getOrNull(pagerState.currentPage)
                val isCurrentVideo = currentItem?.isVideo == true
                val showMoreActionsButton = canDownloadMedia && currentItem != null
                
                LaunchedEffect(isCurrentVideo) {
                    if (!isCurrentVideo) {
                        showVideoSettings = false
                    }
                }
                
                LaunchedEffect(showMoreActionsButton) {
                    if (!showMoreActionsButton) {
                        showMoreActions = false
                    }
                }
                
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
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AnimatedVisibility(
                                visible = isCurrentVideo,
                                enter = expressiveScaleIn,
                                exit = expressiveScaleOut
                            ) {
                                IconButton(
                                    onClick = { showVideoSettings = true },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Settings,
                                        contentDescription = stringResource(R.string.video_settings)
                                    )
                                }
                                AppDropdownMenu(
                                    expanded = showVideoSettings,
                                    onDismissRequest = { showVideoSettings = false }) {
                                    /*
                                     * Текущая скорость — справа, а не вторым словом в названии:
                                     * это состояние пункта, а не его имя.
                                     */
                                    AppDropdownMenuItem(
                                        text = stringResource(R.string.speed),
                                        onClick = {
                                            showVideoSettings = false
                                            showSpeedBottomSheet = true
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Rounded.Speed,
                                                contentDescription = null
                                            )
                                        },
                                        trailingIcon = {
                                            Text(
                                                text = String.format(
                                                    Locale.ROOT, "%.1f", videoPlaybackSpeed
                                                ) + 'x',
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        })
                                    AppDropdownMenuItem(
                                        text = stringResource(R.string.loop),
                                        onClick = {
                                            onVideoLoopingChange(!isVideoLooping)
                                            showVideoSettings = false
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Rounded.Repeat,
                                                contentDescription = null
                                            )
                                        },
                                        contentColor = if (isVideoLooping) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        })
                                }
                            }
                            
                            AnimatedVisibility(
                                visible = showMoreActionsButton,
                                enter = expressiveScaleIn,
                                exit = expressiveScaleOut
                            ) {
                                IconButton(
                                    onClick = { showMoreActions = true },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MoreVert,
                                        contentDescription = stringResource(R.string.actions)
                                    )
                                }
                                AppDropdownMenu(
                                    expanded = showMoreActions,
                                    onDismissRequest = { showMoreActions = false }) {
                                    if (canDownloadMedia && currentItem != null) {
                                        AppDropdownMenuItem(
                                            text = stringResource(R.string.save_to_gallery),
                                            onClick = {
                                                onSaveToGallery(currentItem.uri)
                                                showMoreActions = false
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Rounded.SaveAlt,
                                                    contentDescription = null
                                                )
                                            })
                                    }
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        },
        /* Фон рисует переход из миниатюры: свой у Scaffold только перекрыл бы его. */
        containerColor = Color.Transparent,
        /* Медиа уходит под панель целиком, а её собственный отступ считает сама панель. */
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (media.isEmpty()) {
                CircularWavyProgressIndicator()
            }
            
            HorizontalPager(
                state = pagerState, modifier = Modifier
                    .fillMaxSize()
                    .dismissDragGestures(
                        state = dismissDragState,
                        onTap = { isUiVisible = !isUiVisible },
                        onDismiss = hero::dismiss
                    )
                    .mediaHeroContent(hero)) { page ->
                val item = media.getOrNull(page)
                val isCurrentPage = pagerState.currentPage == page
                
                if (item == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularWavyProgressIndicator()
                    }
                } else {
                    ZoomableMediaPage(
                        uri = item.uri,
                        isVideo = item.isVideo,
                        isCurrentPage = isCurrentPage,
                        pagerState = pagerState,
                        onTap = { isUiVisible = !isUiVisible },
                        isVideoUiVisible = isChromeVisible,
                        isVideoLooping = isVideoLooping,
                        videoPlaybackSpeed = videoPlaybackSpeed,
                        onVideoPlayingChanged = { playing ->
                            isVideoPlaying = playing
                        },
                        onShowVideoUiRequest = {
                            isUiVisible = true
                            lastInteractionTime = System.currentTimeMillis()
                        },
                        onHeroContentSizeChanged = hero::updateContentSize)
                }
            }
            
            /*
             * Затемнение гаснет вместе с панелью: её высоту Scaffold отдаёт рывком, как
             * только панель исчезла, и без этого градиент пропадал бы первым.
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
        }
    }
    
    if (showSpeedBottomSheet) {
        SpeedBottomSheet(
            currentSpeed = videoPlaybackSpeed,
            onSpeedChange = onVideoPlaybackSpeedChange,
            onDismiss = { showSpeedBottomSheet = false })
    }
}

private const val MIN_PLAYBACK_SPEED = 0.1f
private const val MAX_PLAYBACK_SPEED = 10.0f
private const val PAGE_SETTLE_FRACTION = 0.25f

internal suspend fun PagerState.settleAfterEdgePan() {
    val offsetFraction = currentPageOffsetFraction
    val nextPage = when {
        offsetFraction > PAGE_SETTLE_FRACTION -> currentPage + 1
        offsetFraction < -PAGE_SETTLE_FRACTION -> currentPage - 1
        else -> currentPage
    }
    
    animateScrollToPage(nextPage.coerceIn(0, (pageCount - 1).coerceAtLeast(0)))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedBottomSheet(
    currentSpeed: Float, onSpeedChange: (Float) -> Unit, onDismiss: () -> Unit
) {
    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "${String.format(Locale.ROOT, "%.1f", currentSpeed)}x",
                style = MaterialTheme.typography.bodyLarge
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = {
                    val newSpeed = ((currentSpeed - 0.1f) * 10f).roundToInt() / 10f
                    onSpeedChange(newSpeed.coerceAtLeast(MIN_PLAYBACK_SPEED))
                }) {
                    Icon(Icons.Rounded.Remove, contentDescription = "Decrease speed")
                }
                Slider(
                    value = currentSpeed, onValueChange = {
                        val roundedSpeed = (it * 10f).roundToInt() / 10f
                        onSpeedChange(roundedSpeed)
                    },
                    valueRange = MIN_PLAYBACK_SPEED..MAX_PLAYBACK_SPEED,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    val newSpeed = ((currentSpeed + 0.1f) * 10f).roundToInt() / 10f
                    onSpeedChange(newSpeed.coerceAtMost(MAX_PLAYBACK_SPEED))
                }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Increase speed")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val speedValues = remember { listOf(1f, 2.5f, 5f, 7.5f, 10f) }
                speedValues.forEach { value ->
                    OutlinedButton(
                        onClick = {
                            onSpeedChange(value)
                        },
                        shapes = ButtonDefaults.shapes()
                    ) {
                        Text(value.toString())
                    }
                }
            }
        }
    }
}
