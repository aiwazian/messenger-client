/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CropRotate
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberRangeSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import androidx.media3.ui.compose.state.rememberPresentationState
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.animations.expressiveScaleIn
import com.aiwazian.messenger.ui.animations.expressiveScaleOut
import com.aiwazian.messenger.ui.components.MediaCropMask
import com.aiwazian.messenger.ui.components.MediaFlipButton
import com.aiwazian.messenger.ui.components.MediaOverlayIconButton
import com.aiwazian.messenger.ui.components.MediaRotateButton
import com.aiwazian.messenger.ui.components.MediaTransformState
import com.aiwazian.messenger.ui.components.MASK_INSET
import com.aiwazian.messenger.ui.components.VideoCropState
import com.aiwazian.messenger.ui.components.VideoTrimSlider
import com.aiwazian.messenger.ui.components.maskSideFor
import com.aiwazian.messenger.ui.components.mediaHeroBackground
import com.aiwazian.messenger.ui.components.mediaHeroContainer
import com.aiwazian.messenger.ui.components.mediaHeroContent
import com.aiwazian.messenger.ui.components.mediaTransform
import com.aiwazian.messenger.ui.components.pickerMediaKey
import com.aiwazian.messenger.ui.components.rememberMediaHeroState
import com.aiwazian.messenger.ui.components.rememberMediaTransformState
import com.aiwazian.messenger.ui.components.rememberVideoCropState
import com.aiwazian.messenger.utils.media.EncodedVideo
import com.aiwazian.messenger.utils.media.MIN_VIDEO_DURATION_MS
import com.aiwazian.messenger.utils.media.MediaTransform
import com.aiwazian.messenger.utils.media.TrimmedVideoEncoder
import com.aiwazian.messenger.utils.media.VideoExportTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class VideoPickerCropViewModel @Inject constructor(
    val trimmedVideoEncoder: TrimmedVideoEncoder
) : ViewModel()

@OptIn(UnstableApi::class)
@Composable
fun VideoPickerCropDialog(
    uri: Uri,
    maskShape: Shape,
    exportTarget: VideoExportTarget,
    onConfirm: (EncodedVideo) -> Unit,
    onDismiss: () -> Unit,
    viewModel: VideoPickerCropViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val cropState = rememberVideoCropState()
    val transformState = rememberMediaTransformState(bakesContent = false)

    var durationMillis by remember(uri) { mutableLongStateOf(0L) }
    var isTransforming by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var transformOrigin by remember { mutableStateOf(MediaTransform.None) }

    val trimState = rememberRangeSliderState(startValue = 0f, endValue = 1f)

    val hero = rememberMediaHeroState(
        originKey = pickerMediaKey(uri), dragOffsetY = 0f, onDismissed = onDismiss
    )

    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(uri) {
        onDispose { player.release() }
    }

    LaunchedEffect(uri) {
        val info = withContext(Dispatchers.IO) { readVideoInfo(context, uri) }

        if (info == null) {
            onDismiss()

            return@LaunchedEffect
        }

        durationMillis = info.durationMs
        cropState.setSourceDimensions(info.width, info.height)
    }

    LaunchedEffect(player) {
        while (true) {
            if (durationMillis > 0L) {
                val startMs = (trimState.startValue * durationMillis).toLong()
                val endMs = (trimState.endValue * durationMillis).toLong()
                val position = player.currentPosition

                if (position !in startMs..<endMs) {
                    player.seekTo(startMs)
                }
            }

            delay(RANGE_POLL_INTERVAL_MS)
        }
    }

    val openTransform: () -> Unit = {
        transformOrigin = transformState.transform

        isTransforming = true
    }

    val cancelTransform: () -> Unit = {
        isTransforming = false

        coroutineScope.launch {
            val target = transformOrigin

            if (transformState.transform.isMirrored != target.isMirrored) {
                transformState.flip()
            }

            while (transformState.transform.rotationDegrees != target.rotationDegrees) {
                transformState.rotate { cropState.applyOrientation(quarterTurnsOf(it)) }
            }
        }
    }

    val confirmExport: () -> Unit = {
        if (!isExporting && cropState.isReady && durationMillis > 0L) {
            isExporting = true

            coroutineScope.launch {
                val startMs = (trimState.startValue * durationMillis).toLong().coerceAtLeast(0L)
                val endMs = (trimState.endValue * durationMillis).toLong()

                val encoded = viewModel.trimmedVideoEncoder.encode(
                    source = uri,
                    target = exportTarget,
                    startMs = startMs,
                    endMs = endMs,
                    transform = transformState.transform,
                    crop = cropState.cropRect()
                )

                if (encoded == null) {
                    isExporting = false
                } else {
                    onConfirm(encoded)
                }
            }
        }
    }

    val goBack: () -> Unit = {
        if (!isExporting) {
            if (isTransforming) {
                cancelTransform()
            } else {
                hero.dismiss()
            }
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

        val isChromeVisible = hero.isSettled

        Box(
            modifier = Modifier
                .fillMaxSize()
                .mediaHeroBackground(hero, MaterialTheme.colorScheme.surface) { 1f }
                .navigationBarsPadding()
                .mediaHeroContainer(hero)
        ) {
            VideoCropBox(
                state = cropState,
                transformState = transformState,
                player = player,
                modifier = Modifier
                    .fillMaxSize()
                    .mediaHeroContent(hero),
                isGestureEnabled = !transformState.isAnimating && !isExporting
            )

            if (!cropState.isReady) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularWavyProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            AnimatedVisibility(
                visible = isChromeVisible && cropState.isReady,
                modifier = Modifier.fillMaxSize(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                MediaCropMask(maskShape = maskShape, modifier = Modifier.fillMaxSize())
            }

            AnimatedVisibility(
                visible = isChromeVisible,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedVisibility(
                        visible = !isTransforming,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        VideoTrimSlider(
                            videoUri = uri,
                            durationMs = durationMillis,
                            state = trimState,
                            minRangeMs = MIN_VIDEO_DURATION_MS,
                            maxRangeMs = exportTarget.maxDurationMs,
                            onRangeChangeFinished = {
                                if (durationMillis > 0L) {
                                    player.seekTo((trimState.startValue * durationMillis).toLong())
                                }
                            }
                        )
                    }

                    AnimatedContent(
                        targetState = isTransforming,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "video_crop_tools"
                    ) { transforming ->
                        if (transforming) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    MediaFlipButton(state = transformState)

                                    MediaRotateButton(state = transformState) { applied ->
                                        cropState.applyOrientation(quarterTurnsOf(applied))
                                    }
                                }

                                Box(modifier = Modifier.fillMaxWidth()) {
                                    TextButton(
                                        onClick = cancelTransform,
                                        modifier = Modifier.align(Alignment.CenterStart),
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    ) {
                                        Text(text = stringResource(R.string.cancel).uppercase())
                                    }

                                    this@Column.AnimatedVisibility(
                                        visible = transformState.isChanged,
                                        modifier = Modifier.align(Alignment.Center),
                                        enter = expressiveScaleIn,
                                        exit = expressiveScaleOut
                                    ) {
                                        TextButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    transformState.reset {
                                                        cropState.applyOrientation(0)
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.textButtonColors(
                                                contentColor = MaterialTheme.colorScheme.onSurface
                                            )
                                        ) {
                                            Text(text = stringResource(R.string.reset).uppercase())
                                        }
                                    }

                                    TextButton(
                                        onClick = { isTransforming = false },
                                        modifier = Modifier.align(Alignment.CenterEnd)
                                    ) {
                                        Text(text = stringResource(R.string.done).uppercase())
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                MediaOverlayIconButton(
                                    icon = Icons.Rounded.CropRotate,
                                    onClick = openTransform,
                                    modifier = Modifier.align(Alignment.Center),
                                    isActive = transformState.isChanged
                                )

                                MediaOverlayIconButton(
                                    icon = Icons.AutoMirrored.Rounded.Send,
                                    onClick = confirmExport,
                                    modifier = Modifier.align(Alignment.CenterEnd)
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isChromeVisible,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(4.dp),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                IconButton(
                    onClick = goBack, colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null
                    )
                }
            }

            if (isExporting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularWavyProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun VideoCropBox(
    state: VideoCropState,
    transformState: MediaTransformState,
    player: Player?,
    modifier: Modifier = Modifier,
    isGestureEnabled: Boolean
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val transformableState = rememberTransformableState { centroid, zoomChange, panChange, _ ->
        if (!isGestureEnabled) return@rememberTransformableState

        coroutineScope.launch { state.transform(centroid, zoomChange, panChange) }
    }

    BoxWithConstraints(modifier = modifier.transformable(state = transformableState)) {
        val inset = with(density) { MASK_INSET.toPx() }

        val viewportWidth = constraints.maxWidth.toFloat()
        val viewportHeight = constraints.maxHeight.toFloat()

        val side = maskSideFor(viewportWidth, viewportHeight, inset)

        SideEffect {
            state.onMeasured(width = viewportWidth, height = viewportHeight, side = side)
        }

        LaunchedEffect(transformableState.isTransformInProgress) {
            if (!transformableState.isTransformInProgress) {
                state.settle()
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    scaleX = state.scale
                    scaleY = state.scale
                    translationX = state.offsetX.value
                    translationY = state.offsetY.value
                }
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(
                        with(density) { (state.displayWidth * state.fitScale).toDp() },
                        with(density) { (state.displayHeight * state.fitScale).toDp() }
                    )
                    .mediaTransform(
                        state = transformState,
                        contentSize = Size(
                            state.sourceWidth.toFloat(),
                            state.sourceHeight.toFloat()
                        )
                    )
            ) {
                if (player != null) {
                    val presentationState = rememberPresentationState(player)

                    PlayerSurface(
                        player = player,
                        modifier = Modifier.fillMaxSize(),
                        surfaceType = SURFACE_TYPE_TEXTURE_VIEW
                    )

                    if (presentationState.coverSurface) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black)
                        )
                    }
                }
            }
        }
    }
}

private class VideoInfo(
    val width: Int,
    val height: Int,
    val durationMs: Long
)

private fun readVideoInfo(context: Context, uri: Uri): VideoInfo? {
    val retriever = MediaMetadataRetriever()

    return try {
        retriever.setDataSource(context, uri)

        val width = retriever.readMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        val height = retriever.readMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
        val rotation = retriever.readMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
        val durationMs = retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull() ?: 0L

        if (width <= 0 || height <= 0 || durationMs <= 0L) {
            return null
        }

        val isSwapped = rotation == QUARTER_TURN_DEGREES || rotation == THREE_QUARTER_ROTATION

        VideoInfo(
            width = if (isSwapped) height else width,
            height = if (isSwapped) width else height,
            durationMs = durationMs
        )
    } catch (_: Exception) {
        null
    } finally {
        retriever.release()
    }
}

private fun MediaMetadataRetriever.readMetadata(key: Int): Int =
    extractMetadata(key)?.toIntOrNull() ?: 0

private fun quarterTurnsOf(transform: MediaTransform): Int =
    transform.rotationDegrees / QUARTER_TURN_DEGREES

private const val RANGE_POLL_INTERVAL_MS = 34L
private const val QUARTER_TURN_DEGREES = 90
private const val THREE_QUARTER_ROTATION = 270
