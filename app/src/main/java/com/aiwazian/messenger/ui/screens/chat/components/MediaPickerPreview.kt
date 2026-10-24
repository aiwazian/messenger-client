/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import android.annotation.SuppressLint
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CropRotate
import androidx.compose.material.icons.rounded.Hd
import androidx.compose.material.icons.rounded.Sd
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuGroupShapes
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults.rememberTooltipPositionProvider
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.DeviceMediaItem
import com.aiwazian.messenger.domain.ViewOnceOption
import com.aiwazian.messenger.extensions.formatFileSize
import com.aiwazian.messenger.ui.animations.expressiveScaleIn
import com.aiwazian.messenger.ui.animations.expressiveScaleOut
import com.aiwazian.messenger.ui.app.AppDropdownMenuItem
import com.aiwazian.messenger.ui.app.AppDropdownMenuPopup
import com.aiwazian.messenger.ui.components.ColorPickerDialog
import com.aiwazian.messenger.ui.components.MediaDrawControls
import com.aiwazian.messenger.ui.components.MediaDrawLayer
import com.aiwazian.messenger.ui.components.MediaDrawRaster
import com.aiwazian.messenger.ui.components.MediaDrawRasterView
import com.aiwazian.messenger.ui.components.MediaEditorActionButtons
import com.aiwazian.messenger.ui.components.MediaOverlayControl
import com.aiwazian.messenger.ui.components.MediaTransformControls
import com.aiwazian.messenger.ui.components.PlayerSpeedBadge
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
import com.aiwazian.messenger.ui.components.rememberZoomableState
import com.aiwazian.messenger.utils.media.MediaCompressionConfig
import com.aiwazian.messenger.utils.media.MediaTransform
import com.aiwazian.messenger.utils.media.VideoMetadata
import com.aiwazian.messenger.utils.media.VideoQuality
import com.aiwazian.messenger.utils.media.estimateSizeBytes
import com.aiwazian.messenger.utils.media.frameFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

private enum class PreviewMode {
    Content,
    Quality,
    Transform,
    Draw
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalLayoutApi::class)
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
    mediaDrawings: (DeviceMediaItem) -> MediaDrawRaster? = { null },
    onMediaDrawingsChange: (DeviceMediaItem, MediaDrawRaster) -> Unit = { _, _ -> },
    drawColor: Color = Color.Red,
    onDrawColorChange: (Color) -> Unit = {},
    onCurrentItemChange: (DeviceMediaItem?) -> Unit = {},
    isSelectionEnabled: Boolean = true,
    allowViewOnce: Boolean = false,
    viewOnceOption: (DeviceMediaItem) -> ViewOnceOption? = { null },
    onViewOnceOptionChange: (DeviceMediaItem, ViewOnceOption?) -> Unit = { _, _ -> },
    onSend: (() -> Unit)? = null
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
    var isVideoFastForwarding by remember { mutableStateOf(false) }
    
    var transformAttempt by remember { mutableIntStateOf(0) }
    var drawAttempt by remember { mutableIntStateOf(0) }
    var isEraser by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var drawSession by remember { mutableStateOf<MediaDrawRaster?>(null) }
    var mediaContentSize by remember { mutableStateOf(Size.Zero) }
    
    val drawZoomableState = rememberZoomableState()
    
    LaunchedEffect(mode, currentItem?.uri, drawAttempt) {
        if (mode == PreviewMode.Draw) {
            val committed = currentItem?.let(mediaDrawings)
            
            drawSession = committed?.copy()
                ?: mediaContentSize.takeIf { it != Size.Zero }
                    ?.let { MediaDrawRaster.fromContentSize(it) }
        } else {
            drawSession = null
        }
    }
    
    LaunchedEffect(currentItem?.uri) {
        mode = PreviewMode.Content
        draftQuality = null
        drawZoomableState.reset()
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
    
    val isTransformed = currentItem?.let(mediaTransform)?.isIdentity == false
    
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
    
    val canDraw = currentItem != null && !currentItem.isGif
    
    val openDraw: (() -> Unit)? = if (mode == PreviewMode.Content && canDraw) {
        { mode = PreviewMode.Draw }
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
            
            PreviewMode.Draw -> {
                drawAttempt++
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
        
        val isChromeVisible =
            !dismissDragState.isDragging && hero.isSettled && !isVideoFastForwarding
        
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .mediaHeroBackground(
                        hero,
                        MaterialTheme.colorScheme.surface
                    ) { backgroundAlpha }
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
                                if (mode != PreviewMode.Draw && frame != null && estimate != null) {
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
                                MediaOverlayControl {
                                    if (mode == PreviewMode.Draw) {
                                        IconButton(
                                            onClick = { drawSession?.undo() },
                                            enabled = (drawSession?.undoCount ?: 0) > 0
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.Undo,
                                                null
                                            )
                                        }
                                    } else {
                                        IconButton(
                                            onClick = goBack
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                                null
                                            )
                                        }
                                    }
                                }
                            }, actions = {
                                MediaOverlayControl {
                                    AnimatedContent(
                                        targetState = mode
                                    ) { mode ->
                                        if (mode == PreviewMode.Draw) {
                                            Box {
                                                TextButton(
                                                    onClick = { drawSession?.clearAll() },
                                                    enabled = drawSession?.hasInk == true,
                                                    modifier = Modifier.padding(horizontal = 4.dp),
                                                    colors = ButtonDefaults.textButtonColors(
                                                        contentColor = MaterialTheme.colorScheme.onSurface
                                                    )
                                                ) {
                                                    Text(text = stringResource(R.string.draw_clear_all))
                                                }
                                            }
                                        } else {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                AnimatedContent(
                                                    targetState = allowViewOnce &&
                                                            currentItem != null &&
                                                            isSelectionEnabled &&
                                                            !currentItem.isGif &&
                                                            selectionNumber(currentItem) > 0,
                                                    transitionSpec = {
                                                        expressiveScaleIn togetherWith expressiveScaleOut
                                                    },
                                                    contentAlignment = Alignment.Center
                                                ) { showTimer ->
                                                    if (showTimer && currentItem != null) {
                                                        ViewOnceTimerButton(
                                                            option = viewOnceOption(currentItem),
                                                            onOptionChange = { option ->
                                                                onViewOnceOptionChange(
                                                                    currentItem,
                                                                    option
                                                                )
                                                            }
                                                        )
                                                    }
                                                }
                                                
                                                if (currentItem != null && isSelectionEnabled) {
                                                    IconButton(onClick = {
                                                        onToggleSelection(
                                                            currentItem
                                                        )
                                                    }) {
                                                        MediaSelectionBadge(
                                                            number = selectionNumber(currentItem)
                                                        )
                                                    }
                                                }
                                            }
                                        }
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
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            when (mode) {
                                PreviewMode.Transform -> MediaTransformControls(
                                    state = transformState,
                                    onCancel = goBack,
                                    onDone = {
                                        if (currentItem != null) {
                                            onMediaTransformChange(
                                                currentItem,
                                                transformState.transform
                                            )
                                        }
                                        
                                        draftQuality = null
                                        mode = PreviewMode.Content
                                    },
                                    onReset = {
                                        coroutineScope.launch { transformState.reset() }
                                    }
                                )
                                
                                PreviewMode.Draw -> MediaDrawControls(
                                    color = drawColor,
                                    isEraser = isEraser,
                                    onColorClick = { showColorPicker = true },
                                    onEraserSelected = { isEraser = it },
                                    onCancel = goBack,
                                    onDone = {
                                        if (currentItem != null) {
                                            val session = drawSession
                                            
                                            if (session != null) {
                                                onMediaDrawingsChange(
                                                    currentItem,
                                                    session
                                                )
                                            }
                                        }
                                        
                                        mode = PreviewMode.Content
                                    }
                                )
                                
                                PreviewMode.Quality -> {
                                    VideoQualitySlider(
                                        stops = stops, selected = selectedQuality,
                                        onSelect = { draftQuality = it })
                                    
                                    MediaEditorActionButtons(
                                        onCancel = goBack,
                                        onDone = {
                                            if (currentItem != null) {
                                                onVideoQualityChange(
                                                    currentItem, selectedQuality
                                                )
                                            }
                                            
                                            draftQuality = null
                                            mode = PreviewMode.Content
                                        }
                                    )
                                }
                                
                                PreviewMode.Content -> Unit
                            }
                        }
                    }
                },
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets()
            ) {
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
                        val committedRaster = mediaDrawings(item)
                        
                        Box {
                            ZoomableMediaPage(
                                uri = item.uri,
                                isVideo = item.isVideo,
                                isCurrentPage = isCurrentPage,
                                pagerState = pagerState,
                                onTap = {},
                                isPageChangeEnabled = !isEditing,
                                isGesturesEnabled = mode != PreviewMode.Draw,
                                isVideoUiVisible = isChromeVisible && mode != PreviewMode.Draw,
                                isVideoSeekBarVisible = !isEditing,
                                isTransformable = !item.isGif,
                                isTransformed = mediaTransform(item)?.isIdentity == false,
                                videoQualityIcon = qualityIcon,
                                onVideoQualityClick = if (isCurrentPage) openQuality else null,
                                onVideoTransformClick = if (isCurrentPage) openTransform else null,
                                onVideoDrawClick = if (isCurrentPage) openDraw else null,
                                isDrawn = committedRaster?.hasInk == true,
                                transformState = if (isCurrentPage) transformState else null,
                                zoomableStateOverride =
                                    if (isCurrentPage && mode == PreviewMode.Draw) {
                                        drawZoomableState
                                    } else {
                                        null
                                    },
                                drawSurface = when {
                                    isCurrentPage && mode == PreviewMode.Draw -> {
                                        { surfaceOffset ->
                                            drawSession?.let { session ->
                                                MediaDrawLayer(
                                                    raster = session,
                                                    color = drawColor,
                                                    isEraser = isEraser,
                                                    zoomableState = drawZoomableState,
                                                    zoomCoordinateOffset = surfaceOffset,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }
                                    }
                                    
                                    committedRaster != null -> {
                                        { _ ->
                                            MediaDrawRasterView(
                                                raster = committedRaster,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                    
                                    else -> null
                                },
                                onVideoFastForwardChanged = { fastForwarding ->
                                    if (isCurrentPage) {
                                        isVideoFastForwarding = fastForwarding
                                    }
                                },
                                onHeroContentSizeChanged = { size ->
                                    hero.updateContentSize(size)
                                    
                                    if (isCurrentPage) {
                                        mediaContentSize = size
                                    }
                                }
                            )
                        }
                    }
                    
                    AnimatedVisibility(
                        visible = isChromeVisible && mode == PreviewMode.Content &&
                                currentItem != null && !currentItem.isVideo && !currentItem.isGif,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        if (onSend != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceContainer,
                                        shape = CircleShape
                                    )
                            ) {
                                IconButton(onClick = { mode = PreviewMode.Draw }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Brush,
                                        contentDescription = null,
                                        tint = if (currentItem?.let { mediaDrawings(it)?.hasInk } == true) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                                
                                if (openTransform != null) {
                                    IconButton(onClick = { mode = PreviewMode.Transform }) {
                                        Icon(
                                            imageVector = Icons.Rounded.CropRotate,
                                            contentDescription = null,
                                            tint = if (isTransformed) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                }
                                
                                IconButton(onClick = onSend) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Send,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.background(
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    shape = CircleShape
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { mode = PreviewMode.Draw }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Brush,
                                        contentDescription = null,
                                        tint = if (currentItem?.let { mediaDrawings(it)?.hasInk } == true) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                                
                                if (openTransform != null) {
                                    IconButton(onClick = { mode = PreviewMode.Transform }) {
                                        Icon(
                                            imageVector = Icons.Rounded.CropRotate,
                                            contentDescription = null,
                                            tint = if (isTransformed) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            PlayerSpeedBadge(
                speed = PLAYER_FAST_FORWARD_SPEED,
                visible = isVideoFastForwarding,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility)
                    .padding(top = 12.dp)
            )
        }
    }
    
    if (showColorPicker) {
        ColorPickerDialog(
            currentColor = drawColor,
            onColorSelected = { color ->
                onDrawColorChange(color)
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewOnceTimerButton(
    option: ViewOnceOption?,
    onOptionChange: (ViewOnceOption?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var tooltipText by remember { mutableStateOf("") }
    val tooltipState = rememberTooltipState()
    val scope = rememberCoroutineScope()
    val options = listOf(
        null,
        ViewOnceOption(seconds = 3),
        ViewOnceOption(seconds = 10),
        ViewOnceOption(seconds = 30),
        ViewOnceOption(seconds = null)
    )
    
    LaunchedEffect(tooltipState.isVisible) {
        if (tooltipState.isVisible) {
            delay(5.seconds)
            tooltipState.dismiss()
        }
    }
    
    TooltipBox(
        positionProvider = rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
        tooltip = {
            PlainTooltip(
                modifier = Modifier.padding(horizontal = 8.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(text = tooltipText, lineHeight = 12.sp)
            }
        },
        state = tooltipState,
        enableUserInput = false
    ) {
        IconButton(onClick = { expanded = true }) {
            val label = option?.let { it.seconds?.toString() ?: "1" }
            
            if (label == null) {
                Icon(
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = stringResource(R.string.view_once_media_timer)
                )
            } else {
                Text(text = label)
            }
        }
    }
    
    AppDropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier.widthIn(max = 220.dp)
    ) {
        DropdownMenuGroup(
            shapes = MenuGroupShapes(
                MaterialTheme.shapes.medium,
                MaterialTheme.shapes.medium
            ),
            contentPadding = PaddingValues.Zero
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.view_once_hint_timer),
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        
        Spacer(Modifier.height(4.dp))
        
        DropdownMenuGroup(
            shapes = MenuGroupShapes(
                MaterialTheme.shapes.medium,
                MaterialTheme.shapes.medium
            ),
            contentPadding = PaddingValues.Zero
        ) {
            options.forEach { candidate ->
                val candidateSeconds = candidate?.seconds
                val isSelected = if (candidate == null) {
                    option == null
                } else {
                    option != null && candidateSeconds == option.seconds
                }
                val hint = when {
                    candidate == null -> stringResource(R.string.view_once_hint_never)
                    candidateSeconds == null -> stringResource(R.string.view_once_hint_one_view)
                    else -> pluralStringResource(
                        R.plurals.view_once_hint_timed,
                        candidateSeconds,
                        candidateSeconds
                    )
                }
                
                AppDropdownMenuItem(
                    text = when {
                        candidate == null -> stringResource(R.string.view_once_never)
                        candidateSeconds == null -> stringResource(R.string.view_once_one_view)
                        else -> stringResource(R.string.view_once_seconds, candidateSeconds)
                    },
                    trailingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null
                            )
                        }
                    } else {
                        null
                    },
                    onClick = {
                        onOptionChange(candidate)
                        expanded = false
                        tooltipText = hint
                        scope.launch { tooltipState.show() }
                    }
                )
            }
        }
    }
}
