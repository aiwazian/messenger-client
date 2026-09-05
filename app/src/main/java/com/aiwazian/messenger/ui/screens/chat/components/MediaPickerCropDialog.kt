/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CropRotate
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.aiwazian.messenger.ui.components.MediaCropBox
import com.aiwazian.messenger.ui.components.MediaCropMask
import com.aiwazian.messenger.ui.components.MediaFlipButton
import com.aiwazian.messenger.ui.components.MediaOverlayIconButton
import com.aiwazian.messenger.ui.components.MediaRotateButton
import com.aiwazian.messenger.ui.components.mediaHeroBackground
import com.aiwazian.messenger.ui.components.mediaHeroContainer
import com.aiwazian.messenger.ui.components.mediaHeroContent
import com.aiwazian.messenger.ui.components.pickerMediaKey
import com.aiwazian.messenger.ui.components.rememberMediaCropState
import com.aiwazian.messenger.ui.components.rememberMediaHeroState
import com.aiwazian.messenger.ui.components.rememberMediaTransformState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun MediaPickerCropDialog(
    uri: Uri,
    maskShape: Shape,
    onConfirm: (Uri) -> Unit,
    onDismiss: () -> Unit,
    clipsToMask: Boolean = false
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()
    
    val cropState = rememberMediaCropState(uri)
    val transformState = rememberMediaTransformState(bakesContent = true)
    
    var isTransforming by remember { mutableStateOf(false) }
    var isConfirming by remember { mutableStateOf(false) }
    
    val hero = rememberMediaHeroState(
        originKey = pickerMediaKey(uri), dragOffsetY = 0f, onDismissed = onDismiss
    )
    
    val goBack = {
        if (isTransforming) {
            isTransforming = false
        } else {
            hero.dismiss()
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
            if (cropState.bitmap == null) {
                Box(
                    modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) {
                    CircularWavyProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                MediaCropBox(
                    state = cropState,
                    modifier = Modifier
                        .fillMaxSize()
                        .mediaHeroContent(hero),
                    contentRotation = transformState.contentRotation,
                    contentScaleX = transformState.contentScaleX,
                    isGestureEnabled = !transformState.isAnimating
                )
            }
            
            AnimatedVisibility(
                visible = isChromeVisible && cropState.bitmap != null,
                modifier = Modifier.fillMaxSize(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                MediaCropMask(maskShape = maskShape, modifier = Modifier.fillMaxSize())
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
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null
                    )
                }
            }
            
            AnimatedVisibility(
                visible = isChromeVisible,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    AnimatedContent(
                        targetState = isTransforming,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        modifier = Modifier.align(Alignment.Center),
                        label = "media_crop_tools"
                    ) { transforming ->
                        if (transforming) {
                            HorizontalFloatingToolbar(
                                expanded = true, floatingActionButton = {
                                    FloatingActionButton(
                                        onClick = { isTransforming = false },
                                        shape = CircleShape,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Done,
                                            contentDescription = null
                                        )
                                    }
                                }) {
                                MediaRotateButton(state = transformState) {
                                    coroutineScope.launch { cropState.rotate() }
                                }
                                
                                MediaFlipButton(state = transformState) {
                                    cropState.mirror()
                                }
                            }
                        } else {
                            MediaOverlayIconButton(
                                icon = Icons.Rounded.CropRotate,
                                onClick = { isTransforming = true },
                                isActive = cropState.isTransformed)
                        }
                    }
                    
                    AnimatedVisibility(
                        visible = !isTransforming,
                        modifier = Modifier.align(Alignment.CenterEnd),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        MediaOverlayIconButton(
                            icon = Icons.AutoMirrored.Rounded.Send,
                            onClick = {
                                if (isConfirming || !cropState.isReady) {
                                    return@MediaOverlayIconButton
                                }
                                
                                isConfirming = true
                                
                                coroutineScope.launch {
                                    val cropped = if (clipsToMask) {
                                        cropState.crop(maskShape, density, layoutDirection)
                                    } else {
                                        cropState.crop()
                                    }
                                    
                                    val target = if (cropped == null) {
                                        null
                                    } else {
                                        withContext(Dispatchers.IO) { writeCrop(context, cropped) }
                                    }
                                    
                                    if (target == null) {
                                        isConfirming = false
                                    } else {
                                        onConfirm(target)
                                    }
                                }
                            })
                    }
                }
            }
        }
    }
}

private fun writeCrop(context: Context, bitmap: Bitmap): Uri? {
    return try {
        val directory = File(context.cacheDir, CROP_DIRECTORY_NAME)
        
        directory.mkdirs()
        dropStale(directory)
        
        val target = File(directory, "$CROP_NAME_PREFIX${System.currentTimeMillis()}.png")
        
        FileOutputStream(target).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, stream)
        }
        
        Uri.fromFile(target)
    } catch (e: Exception) {
        Log.w(TAG, "Failed to store cropped frame", e)
        
        null
    }
}

private fun dropStale(directory: File) {
    val deadline = System.currentTimeMillis() - CROP_MAX_AGE_MS
    
    directory.listFiles()?.forEach { file ->
        if (file.lastModified() < deadline) {
            file.delete()
        }
    }
}

private const val TAG = "MediaPickerCrop"
private const val CROP_DIRECTORY_NAME = "media_crops"
private const val CROP_NAME_PREFIX = "crop_"
private const val PNG_QUALITY = 100
private const val CROP_MAX_AGE_MS = 6L * 60 * 60 * 1000
