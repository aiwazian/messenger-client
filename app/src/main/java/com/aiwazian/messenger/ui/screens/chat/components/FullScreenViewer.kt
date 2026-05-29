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
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularWavyProgressIndicator
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame
import coil.compose.AsyncImage
import com.aiwazian.messenger.extensions.getFileType
import com.aiwazian.messenger.ui.components.PlayerUi
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun FullScreenViewer(
    mediaUris: List<Uri?>,
    initialPage: Int,
    onDismiss: () -> Unit
) {
    var isUiVisible by remember { mutableStateOf(true) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    
    val dismissThresholdPx = 300f
    
    val animatedOffsetY by animateFloatAsState(
        targetValue = dragOffsetY,
        animationSpec = if (isDragging) snap()
        else spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "photoOffsetY"
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
        onDispose { insetsController.show(WindowInsetsCompat.Type.statusBars()) }
    }
    
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { mediaUris.size })
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = backgroundAlpha)),
        contentAlignment = Alignment.Center
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
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
                            
                            val change = event.changes.firstOrNull { it.id == pointerId }
                                ?: event.changes.firstOrNull()
                                ?: break
                            
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
                }
        ) { page ->
            val uri = mediaUris[page]
            val isCurrentPage = pagerState.currentPage == page
            
            if (uri == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularWavyProgressIndicator()
                }
            } else if (uri.getFileType(context).startsWith("video/")) {
                VideoPlayerItem(
                    uri = uri,
                    isCurrentPage = isCurrentPage,
                    isUiVisible = isUiVisible
                )
            } else {
                AsyncImage(
                    model = uri,
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
        ) {
            TopAppBar(
                modifier = Modifier
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                Color.Transparent
                            )
                        )
                    ),
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick = onDismiss,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack, null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    }
}

@Composable
fun VideoPlayerItem(
    uri: Uri,
    isCurrentPage: Boolean,
    isUiVisible: Boolean
) {
    val context = LocalContext.current
    
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
        }
    }
    
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }
    
    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage && isPlaying) {
            player.pause()
        }
    }
    
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    duration = player.duration.coerceAtLeast(0L)
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    
    LaunchedEffect(isPlaying, isSeeking) {
        while (isPlaying && !isSeeking) {
            currentPosition = player.currentPosition.coerceAtLeast(0L)
            delay(16L)
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        ContentFrame(
            player = player,
            modifier = Modifier.fillMaxSize()
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
                isSeeking = isSeeking,
                onSeekBarPositionChange = { newPos ->
                    isSeeking = true
                    currentPosition = newPos
                },
                onSeekBarPositionChangeFinished = {
                    player.seekTo(currentPosition)
                    isSeeking = false
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
                }
            )
        }
    }
}
