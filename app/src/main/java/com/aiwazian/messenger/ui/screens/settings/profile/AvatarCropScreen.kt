/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.Rotate90DegreesCcw
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AvatarCropScreen(
    imageUri: Uri,
    onCropConfirmed: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(imageUri) {
        withContext(Dispatchers.IO) {
            bitmap = loadSampledBitmap(context, imageUri)
        }
    }
    
    BackHandler {
        onDismiss()
    }
    
    if (bitmap == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularWavyProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else {
        AvatarCropContent(
            bitmap = bitmap!!, onCropConfirmed = onCropConfirmed, onDismiss = onDismiss
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AvatarCropContent(
    bitmap: Bitmap, onCropConfirmed: (Bitmap) -> Unit, onDismiss: () -> Unit
) {
    var displayBitmap by remember { mutableStateOf(bitmap) }
    
    var scale by remember { mutableFloatStateOf(1f) }
    val animOffsetX = remember { Animatable(0f) }
    val animOffsetY = remember { Animatable(0f) }
    
    val animRotation = remember { Animatable(0f) }
    var isRotating by remember { mutableStateOf(false) }
    
    val animFlipX = remember { Animatable(1f) }
    var isFlipping by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    var minScale by remember { mutableFloatStateOf(1f) }
    
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        if (isRotating || isFlipping) return@rememberTransformableState
        scale = (scale * zoomChange).coerceIn(minScale, 10f)
        scope.launch {
            animOffsetX.snapTo(animOffsetX.value + panChange.x)
            animOffsetY.snapTo(animOffsetY.value + panChange.y)
        }
    }
    
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .transformable(state = transformableState)
    ) {
        val screenW = constraints.maxWidth.toFloat()
        val screenH = constraints.maxHeight.toFloat()
        
        val circleRadius = minOf(screenW, screenH) * 0.5f
        val fitScale = minOf(screenW / displayBitmap.width, screenH / displayBitmap.height)
        
        val computedMinScale = ((2f * circleRadius) /
                minOf(displayBitmap.width * fitScale, displayBitmap.height * fitScale))
            .coerceAtLeast(1f)
        
        SideEffect {
            minScale = computedMinScale
            if (scale < computedMinScale) scale = computedMinScale
        }
        
        LaunchedEffect(transformableState.isTransformInProgress) {
            if (!transformableState.isTransformInProgress) {
                val imgHalfW = displayBitmap.width * fitScale * scale / 2f
                val imgHalfH = displayBitmap.height * fitScale * scale / 2f
                
                val maxOffX = (imgHalfW - circleRadius).coerceAtLeast(0f)
                val maxOffY = (imgHalfH - circleRadius).coerceAtLeast(0f)
                
                val snapSpec = spring<Float>(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
                launch {
                    animOffsetX.animateTo(
                        animOffsetX.value.coerceIn(-maxOffX, maxOffX),
                        animationSpec = snapSpec
                    )
                }
                launch {
                    animOffsetY.animateTo(
                        animOffsetY.value.coerceIn(-maxOffY, maxOffY),
                        animationSpec = snapSpec
                    )
                }
            }
        }
        
        Image(
            bitmap = displayBitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale * animFlipX.value
                    scaleY = scale
                    translationX = animOffsetX.value
                    translationY = animOffsetY.value
                    rotationZ = animRotation.value
                })
        
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
            drawRect(color = Color.Black.copy(alpha = 0.62f))
            drawCircle(
                color = Color.Transparent,
                radius = circleRadius,
                center = center,
                blendMode = BlendMode.Clear
            )
        }
        
        HorizontalFloatingToolbar(
            expanded = true,
            modifier = Modifier.align(Alignment.BottomCenter),
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        val cropped = cropBitmapToCircleArea(
                            bitmap = displayBitmap,
                            userScale = scale,
                            fitScale = fitScale,
                            offset = Offset(animOffsetX.value, animOffsetY.value),
                            circleRadius = circleRadius,
                        )
                        onCropConfirmed(cropped)
                    },
                    shape = CircleShape,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Rounded.Done, null)
                }
            }
        ) {
            IconButton(
                onClick = {
                    if (isFlipping || isRotating) return@IconButton
                    scope.launch {
                        isRotating = true
                        
                        val oldFitScale = minOf(
                            screenW / displayBitmap.width,
                            screenH / displayBitmap.height
                        )
                        
                        animRotation.animateTo(
                            targetValue = -90f,
                            animationSpec = tween(
                                durationMillis = 300,
                                easing = FastOutSlowInEasing
                            )
                        )
                        
                        displayBitmap = rotateBitmap(displayBitmap)
                        animRotation.snapTo(0f)
                        
                        val newFitScale = minOf(
                            screenW / displayBitmap.width,
                            screenH / displayBitmap.height
                        )
                        val newMinScale = ((2f * circleRadius) / minOf(
                            displayBitmap.width * newFitScale,
                            displayBitmap.height * newFitScale
                        )).coerceAtLeast(1f)
                        
                        scale = (scale * oldFitScale / newFitScale).coerceAtLeast(newMinScale)
                        
                        val snapSpec = spring<Float>(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                        launch { animOffsetX.animateTo(0f, animationSpec = snapSpec) }
                        launch { animOffsetY.animateTo(0f, animationSpec = snapSpec) }
                        
                        isRotating = false
                    }
                }
            ) {
                Icon(Icons.Rounded.Rotate90DegreesCcw, null)
            }
            IconButton(
                onClick = {
                    if (isFlipping || isRotating) return@IconButton
                    scope.launch {
                        isFlipping = true
                        
                        animFlipX.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(
                                durationMillis = 150,
                                easing = FastOutSlowInEasing
                            )
                        )
                        
                        displayBitmap = flipBitmap(displayBitmap)
                        animFlipX.snapTo(0f)
                        
                        animFlipX.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = 150,
                                easing = FastOutSlowInEasing
                            )
                        )
                        
                        isFlipping = false
                    }
                }
            ) {
                Icon(Icons.Rounded.Flip, null)
            }
        }
    }
}

private fun cropBitmapToCircleArea(
    bitmap: Bitmap,
    userScale: Float,
    fitScale: Float,
    offset: Offset,
    circleRadius: Float,
): Bitmap {
    val totalScale = userScale * fitScale
    
    val cropPx = (2f * circleRadius / totalScale).toInt().coerceAtLeast(1)
    
    val bxLeft = ((-circleRadius - offset.x) / totalScale + bitmap.width / 2f).toInt()
    val byTop = ((-circleRadius - offset.y) / totalScale + bitmap.height / 2f).toInt()
    
    val safeX = bxLeft.coerceIn(0, (bitmap.width - 1).coerceAtLeast(0))
    val safeY = byTop.coerceIn(0, (bitmap.height - 1).coerceAtLeast(0))
    val safeSize = cropPx.coerceIn(
        1, minOf(bitmap.width - safeX, bitmap.height - safeY).coerceAtLeast(1)
    )
    
    return Bitmap.createBitmap(bitmap, safeX, safeY, safeSize, safeSize)
}

private fun loadSampledBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        
        val sampleSize = computeSampleSize(boundsOpts.outWidth, boundsOpts.outHeight, 2048)
        
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        }
    } catch (_: Exception) {
        null
    }
}

private fun computeSampleSize(srcWidth: Int, srcHeight: Int, maxDimension: Int): Int {
    var sample = 1
    while (srcWidth / (sample * 2) >= maxDimension || srcHeight / (sample * 2) >= maxDimension) {
        sample *= 2
    }
    return sample
}

private fun rotateBitmap(src: Bitmap): Bitmap {
    val matrix = Matrix().apply { postRotate(-90f) }
    return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
}

private fun flipBitmap(src: Bitmap): Bitmap {
    val matrix = Matrix().apply { postScale(-1f, 1f, src.width / 2f, src.height / 2f) }
    return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
}
