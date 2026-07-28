/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import android.app.Activity
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.request.ImageRequest
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.components.CustomBottomSheet
import com.aiwazian.messenger.ui.components.CustomDropdownMenu
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

/**
 * Одна страница просмотрщика.
 *
 * Тип приходит из вложения сообщения, а не угадывается по URI: у файлов,
 * скачанных с сервера, имени вида `<fileId>` часто вообще без расширения,
 * и mime-тип определялся как application/octet-stream — видео и гифки
 * показывались пустым экраном.
 */
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
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var showVideoSettings by remember { mutableStateOf(false) }
    var showSpeedBottomSheet by remember { mutableStateOf(false) }
    var showMoreActions by remember { mutableStateOf(false) }
    var isVideoPlaying by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    val dismissThresholdPx = 300f
    
    val animatedOffsetY by animateFloatAsState(
        targetValue = dragOffsetY, animationSpec = if (isDragging) snap()
        else spring(
            dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium
        ), label = "photoOffsetY"
    )
    
    val backgroundAlpha by animateFloatAsState(
        targetValue = (1f - (abs(dragOffsetY) / dismissThresholdPx).coerceIn(0f, 1f)),
        label = "backgroundAlpha"
    )
    
    val view = LocalView.current
    val window = remember { (view.context as Activity).window }
    val insetsController = remember {
        WindowCompat.getInsetsController(window, view).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
    
    val context = LocalContext.current
    
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
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = backgroundAlpha))
            .navigationBarsPadding()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        lastInteractionTime = System.currentTimeMillis()
                    }
                }
            }, contentAlignment = Alignment.Center
    ) {
        if (media.isEmpty()) {
            CircularWavyProgressIndicator()
        }
        
        HorizontalPager(
            state = pagerState, modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val pointerId = down.id
                        
                        var previousY = down.position.y
                        var dragDetected = false
                        var totalDragY = 0f
                        var totalDragX = 0f
                        var isHorizontalScroll = false
                        var wasConsumed = false
                        
                        while (true) {
                            val event = awaitPointerEvent()
                            
                            val change =
                                event.changes.firstOrNull { it.id == pointerId }
                                    ?: event.changes.firstOrNull() ?: break
                            
                            if (change.isConsumed) {
                                wasConsumed = true
                                previousY = change.position.y
                                if (!change.pressed) break
                                continue
                            }
                            
                            val dy = change.position.y - previousY
                            val dx = change.position.x - down.position.x
                            previousY = change.position.y
                            
                            if (!dragDetected && !isHorizontalScroll) {
                                totalDragY += dy
                                totalDragX = dx
                                
                                if (abs(totalDragX) > viewConfiguration.touchSlop && abs(totalDragX) > abs(
                                        totalDragY
                                    )
                                ) {
                                    isHorizontalScroll = true
                                } else if (abs(totalDragY) > viewConfiguration.touchSlop) {
                                    dragDetected = true
                                    isDragging = true
                                    dragOffsetY = totalDragY
                                    change.consume()
                                }
                            } else if (dragDetected) {
                                change.consume()
                                totalDragY += dy
                                dragOffsetY = totalDragY
                            }
                            
                            if (!change.pressed) break
                        }
                        
                        when {
                            !dragDetected && !isHorizontalScroll && !wasConsumed -> {
                                isUiVisible = !isUiVisible
                            }
                            
                            abs(totalDragY) > dismissThresholdPx && dragDetected -> {
                                onDismiss()
                            }
                            
                            else -> {
                                isDragging = false
                                dragOffsetY = 0f
                            }
                        }
                        
                        isDragging = false
                    }
                }
                .graphicsLayer {
                    translationY = animatedOffsetY
                }) { page ->
            val item = media.getOrNull(page)
            val isCurrentPage = pagerState.currentPage == page
            
            if (item == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularWavyProgressIndicator()
                }
            } else if (item.isVideo) {
                VideoPlayerItem(
                    uri = item.uri,
                    isCurrentPage = isCurrentPage,
                    isUiVisible = !isDragging && isUiVisible,
                    isLooping = isVideoLooping,
                    playbackSpeed = videoPlaybackSpeed,
                    onPlayingChanged = { playing ->
                        isVideoPlaying = playing
                    },
                    onShowUiRequest = {
                        isUiVisible = true
                        lastInteractionTime = System.currentTimeMillis()
                    })
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.uri)
                        .decoderFactory(GifDecoder.Factory())
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        AnimatedVisibility(
            visible = !isDragging && isUiVisible,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
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
                        onClick = onDismiss, colors = IconButtonDefaults.iconButtonColors(
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
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut()
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
                            CustomDropdownMenu(
                                expanded = showVideoSettings,
                                onDismissRequest = { showVideoSettings = false }) {
                                DropdownMenuItem(text = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(text = stringResource(R.string.speed))
                                        
                                        Text(
                                            text = String.format(
                                                Locale.ROOT, "%.1f", videoPlaybackSpeed
                                            ) + 'x',
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }, onClick = {
                                    showVideoSettings = false
                                    showSpeedBottomSheet = true
                                }, leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Speed,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                })
                                DropdownMenuItem(text = {
                                    Text(
                                        text = stringResource(R.string.loop),
                                        color = if (isVideoLooping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }, onClick = {
                                    onVideoLoopingChange(!isVideoLooping)
                                    showVideoSettings = false
                                }, leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Repeat,
                                        contentDescription = null,
                                        tint = if (isVideoLooping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                })
                            }
                        }
                        
                        AnimatedVisibility(
                            visible = showMoreActionsButton,
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut()
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
                            CustomDropdownMenu(
                                expanded = showMoreActions,
                                onDismissRequest = { showMoreActions = false }) {
                                if (canDownloadMedia && currentItem != null) {
                                    DropdownMenuItem(text = {
                                        Text(
                                            text = stringResource(R.string.save_to_gallery),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }, onClick = {
                                        onSaveToGallery(currentItem.uri)
                                        showMoreActions = false
                                    }, leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.SaveAlt,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    })
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    )
                )
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedBottomSheet(
    currentSpeed: Float, onSpeedChange: (Float) -> Unit, onDismiss: () -> Unit
) {
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    CustomBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
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
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
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
