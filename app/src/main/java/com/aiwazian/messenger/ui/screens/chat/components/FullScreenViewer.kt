/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import android.app.Activity
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.components.CustomDropdownMenu
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun FullScreenViewer(
    mediaUris: List<Uri?>,
    initialPage: Int,
    onSaveToGallery: suspend (Uri) -> Boolean,
    onDismiss: () -> Unit
) {
    var isUiVisible by remember { mutableStateOf(true) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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

    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { mediaUris.size })

    val savedMessage = stringResource(R.string.saved_to_gallery)
    val failedMessage = stringResource(R.string.failed_to_save_to_gallery)

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
                        // Ловим нажатие
                        val down = awaitFirstDown(requireUnconsumed = false)

                        // Ручной трекинг позиции — не зависим от consume() других обработчиков
                        var previousY = down.position.y
                        var dragDetected = false
                        var totalDragY = 0f
                        var totalDragX = 0f
                        var isHorizontalScroll = false

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break

                            // dy вычисляем сами — всегда актуально независимо от consumed
                            val dy = change.position.y - previousY
                            val dx = change.position.x - down.position.x
                            previousY = change.position.y

                            if (!dragDetected && !isHorizontalScroll) {
                                // Накапливаем пока не превысили touchSlop
                                totalDragY += dy
                                totalDragX = dx

                                if (abs(totalDragX) > viewConfiguration.touchSlop && abs(totalDragX) > abs(
                                        totalDragY
                                    )
                                ) {
                                    // Это горизонтальный скролл
                                    isHorizontalScroll = true
                                } else if (abs(totalDragY) > viewConfiguration.touchSlop) {
                                    dragDetected = true
                                    isDragging = true
                                    dragOffsetY = totalDragY
                                    change.consume()
                                }
                            } else if (dragDetected) {
                                // Drag уже идёт — обновляем offset
                                change.consume()
                                totalDragY += dy
                                dragOffsetY = totalDragY
                            }

                            // Палец поднят — выходим из цикла
                            if (!change.pressed) break
                        }

                        when {
                            // Не было drag и не было горизонтального скролла — это тап, переключаем UI
                            !dragDetected && !isHorizontalScroll -> {
                                isUiVisible = !isUiVisible
                            }
                            // Drag был достаточным — закрываем
                            abs(totalDragY) > dismissThresholdPx && dragDetected -> {
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
        ) { page ->
            val uri = mediaUris[page]
            if (uri == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
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
                actions = {
                    IconButton(
                        onClick = { menuExpanded = true },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = null
                        )
                    }
                    CustomDropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = null
                                )
                            },
                            text = { Text(stringResource(R.string.save_to_gallery)) },
                            onClick = {
                                menuExpanded = false
                                val uri = mediaUris.getOrNull(pagerState.currentPage) ?: return@DropdownMenuItem
                                scope.launch {
                                    val ok = onSaveToGallery(uri)
                                    Toast.makeText(
                                        context,
                                        if (ok) savedMessage else failedMessage,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    }
}
