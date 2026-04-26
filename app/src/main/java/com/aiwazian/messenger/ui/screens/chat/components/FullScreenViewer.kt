/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import android.app.Activity
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import com.aiwazian.messenger.R
import com.aiwazian.messenger.extensions.sharedBounds
import com.aiwazian.messenger.ui.components.topBar.DropdownMenuAction
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import kotlin.math.abs

@Composable
fun FullScreenViewer(
    imageUrl: String,
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
            dampingRatio = Spring.DampingRatioMediumBouncy,
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
    
    LaunchedEffect(isUiVisible) {
        if (isUiVisible) insetsController.show(WindowInsetsCompat.Type.statusBars())
        else insetsController.hide(WindowInsetsCompat.Type.statusBars())
    }
    
    DisposableEffect(Unit) {
        onDispose { insetsController.show(WindowInsetsCompat.Type.statusBars()) }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = backgroundAlpha)),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .sharedBounds(key = imageUrl)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        // Ловим нажатие
                        val down = awaitFirstDown(requireUnconsumed = false)
                        
                        // Ручной трекинг позиции — не зависим от consume() других обработчиков
                        var previousY = down.position.y
                        var dragDetected = false
                        var totalDragY = 0f
                        
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            
                            // dy вычисляем сами — всегда актуально независимо от consumed
                            val dy = change.position.y - previousY
                            previousY = change.position.y
                            
                            if (!dragDetected) {
                                // Накапливаем пока не превысили touchSlop
                                totalDragY += dy
                                if (abs(totalDragY) > viewConfiguration.touchSlop) {
                                    dragDetected = true
                                    isDragging = true
                                    dragOffsetY = totalDragY
                                    change.consume()
                                }
                            } else {
                                // Drag уже идёт — обновляем offset
                                change.consume()
                                totalDragY += dy
                                dragOffsetY = totalDragY
                            }
                            
                            // Палец поднят — выходим из цикла
                            if (!change.pressed) break
                        }
                        
                        when {
                            // Не было drag — это тап, переключаем UI
                            !dragDetected -> {
                                isUiVisible = !isUiVisible
                            }
                            // Drag был достаточным — закрываем
                            abs(totalDragY) > dismissThresholdPx -> {
                                onDismiss()
                            }
                            // Drag был мал — возвращаем на место
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
        )
        AnimatedVisibility(
            visible = !isDragging && isUiVisible,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
        ) {
            PageTopBar(
                navigationIcon = NavigationIcon(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = onDismiss
                ),
                actions = listOf(
                    TopBarAction(
                        icon = Icons.Rounded.MoreVert, dropdownActions = listOf(
                            DropdownMenuAction(
                                icon = Icons.Rounded.Download,
                                textResId = R.string.save_to_downloads,
                                onClick = {})
                        )
                    )
                ),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    }
}
