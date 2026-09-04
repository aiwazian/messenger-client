/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas as GraphicsCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.aiwazian.messenger.utils.media.mirrored
import com.aiwazian.messenger.utils.media.rotatedQuarter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MediaCropState internal constructor() {
    
    var bitmap by mutableStateOf<Bitmap?>(null)
        private set
    
    internal var scale by mutableFloatStateOf(1f)
    
    internal var minScale by mutableFloatStateOf(1f)
        private set
    
    internal val offsetX = Animatable(0f)
    internal val offsetY = Animatable(0f)
    
    private var rotationTurns by mutableIntStateOf(0)
    private var isMirrored by mutableStateOf(false)
    
    val isTransformed: Boolean
        get() = rotationTurns % FULL_TURN_STEPS != 0 || isMirrored
    
    private var fitScale = 1f
    
    private var maskSide = 0f
    private var viewportWidth = 0f
    private var viewportHeight = 0f
    
    val isReady: Boolean
        get() = bitmap != null && maskSide > 0f
    
    internal suspend fun setBitmap(value: Bitmap?) {
        bitmap = value
        scale = 1f
        rotationTurns = 0
        isMirrored = false
        
        offsetX.snapTo(0f)
        offsetY.snapTo(0f)
        
        value?.let { applyLimits(it) }
    }
    
    internal fun onMeasured(width: Float, height: Float, side: Float) {
        viewportWidth = width
        viewportHeight = height
        maskSide = side
        
        bitmap?.let { applyLimits(it) }
    }
    
    internal suspend fun transform(centroid: Offset, zoom: Float, pan: Offset) {
        val previousScale = scale
        val nextScale = (previousScale * zoom).coerceIn(minScale, maxOf(minScale, MAX_SCALE))
        val scaleDelta = if (previousScale <= 0f) 1f else nextScale / previousScale
        
        val anchor = Offset(
            x = centroid.x - viewportWidth / 2f, y = centroid.y - viewportHeight / 2f
        )
        
        val moved = zoomAnchoredOffset(
            offset = Offset(offsetX.value, offsetY.value),
            anchor = anchor,
            pan = pan,
            scaleDelta = scaleDelta
        )
        
        scale = nextScale
        
        offsetX.snapTo(moved.x)
        offsetY.snapTo(moved.y)
    }
    
    internal suspend fun settle() = coroutineScope {
        val source = bitmap ?: return@coroutineScope
        
        val halfWidth = source.width * fitScale * scale / 2f
        val halfHeight = source.height * fitScale * scale / 2f
        
        val maxOffsetX = (halfWidth - maskSide / 2f).coerceAtLeast(0f)
        val maxOffsetY = (halfHeight - maskSide / 2f).coerceAtLeast(0f)
        
        launch {
            offsetX.animateTo(offsetX.value.coerceIn(-maxOffsetX, maxOffsetX), SNAP_SPEC)
        }
        
        launch {
            offsetY.animateTo(offsetY.value.coerceIn(-maxOffsetY, maxOffsetY), SNAP_SPEC)
        }
    }
    
    suspend fun rotate() {
        val source = bitmap ?: return
        val previousFitScale = fitScale
        
        val rotated = source.rotatedQuarter()
        
        bitmap = rotated
        rotationTurns += 1
        
        applyLimits(rotated)
        
        if (previousFitScale > 0f && fitScale > 0f) {
            scale = (scale * previousFitScale / fitScale).coerceAtLeast(minScale)
        }
        
        coroutineScope {
            launch { offsetX.animateTo(0f, SNAP_SPEC) }
            launch { offsetY.animateTo(0f, SNAP_SPEC) }
        }
    }
    
    fun mirror() {
        bitmap = bitmap?.mirrored()
        isMirrored = !isMirrored
    }
    
    fun crop(): Bitmap? {
        val source = bitmap ?: return null
        val totalScale = fitScale * scale
        
        if (maskSide <= 0f || totalScale <= 0f) {
            return null
        }
        
        val side = (maskSide / totalScale).toInt().coerceAtLeast(1)
        
        val left = ((-maskSide / 2f - offsetX.value) / totalScale + source.width / 2f).toInt()
        val top = ((-maskSide / 2f - offsetY.value) / totalScale + source.height / 2f).toInt()
        
        val safeLeft = left.coerceIn(0, (source.width - 1).coerceAtLeast(0))
        val safeTop = top.coerceIn(0, (source.height - 1).coerceAtLeast(0))
        
        val safeSide = side.coerceIn(
            1, minOf(source.width - safeLeft, source.height - safeTop).coerceAtLeast(1)
        )
        
        return Bitmap.createBitmap(source, safeLeft, safeTop, safeSide, safeSide)
    }
    
    fun crop(shape: Shape, density: Density, layoutDirection: LayoutDirection): Bitmap? {
        val square = crop() ?: return null
        
        if (maskSide <= 0f) {
            return square
        }
        
        val shapeDensity = Density(
            density = density.density * square.width / maskSide, fontScale = density.fontScale
        )
        
        val clipped = square.clippedTo(shape, shapeDensity, layoutDirection)
        
        if (clipped !== square) {
            square.recycle()
        }
        
        return clipped
    }
    
    private fun applyLimits(source: Bitmap) {
        if (viewportWidth <= 0f || viewportHeight <= 0f || maskSide <= 0f) {
            return
        }
        
        if (source.width <= 0 || source.height <= 0) {
            return
        }
        
        fitScale = minOf(viewportWidth / source.width, viewportHeight / source.height)
        
        val shortSide = minOf(source.width * fitScale, source.height * fitScale)
        
        minScale = if (shortSide > 0f) maskSide / shortSide else 1f
        
        if (scale < minScale) {
            scale = minScale
        }
    }
}

@Composable
fun rememberMediaCropState(uri: Uri): MediaCropState {
    val context = LocalContext.current
    val state = remember(uri) { MediaCropState() }
    
    LaunchedEffect(uri) {
        val loaded = withContext(Dispatchers.IO) { loadSampledBitmap(context, uri) }
        
        state.setBitmap(loaded)
    }
    
    return state
}

@Composable
fun MediaCropBox(
    state: MediaCropState,
    modifier: Modifier = Modifier,
    contentRotation: Float = 0f,
    contentScaleX: Float = 1f,
    isGestureEnabled: Boolean = true
) {
    val coroutineScope = rememberCoroutineScope()
    
    val transformableState = rememberTransformableState { centroid, zoomChange, panChange, _ ->
        if (!isGestureEnabled) return@rememberTransformableState
        
        coroutineScope.launch { state.transform(centroid, zoomChange, panChange) }
    }
    
    BoxWithConstraints(modifier = modifier.transformable(state = transformableState)) {
        val inset = with(LocalDensity.current) { MASK_INSET.toPx() }
        
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
        
        val source = state.bitmap
        
        if (source != null) {
            Image(
                bitmap = source.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = state.scale * contentScaleX
                        scaleY = state.scale
                        translationX = state.offsetX.value
                        translationY = state.offsetY.value
                        rotationZ = contentRotation
                    })
        }
    }
}

@Composable
fun MediaCropMask(maskShape: Shape, modifier: Modifier = Modifier) {
    val scrimColor = MaterialTheme.colorScheme.surface.copy(alpha = SCRIM_ALPHA)
    val layoutDirection = LocalLayoutDirection.current
    
    Canvas(
        modifier = modifier.graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        }) {
        val side = maskSideFor(size.width, size.height, MASK_INSET.toPx())
        
        drawRect(color = scrimColor)
        
        val outline = maskShape.createOutline(
            size = Size(side, side), layoutDirection = layoutDirection, density = this
        )
        
        translate(left = (size.width - side) / 2f, top = (size.height - side) / 2f) {
            drawOutline(outline = outline, color = Color.Transparent, blendMode = BlendMode.Clear)
        }
    }
}

private fun maskSideFor(width: Float, height: Float, inset: Float): Float =
    (minOf(width, height) - inset * 2f).coerceAtLeast(1f)

private fun Bitmap.clippedTo(
    shape: Shape, density: Density, layoutDirection: LayoutDirection
): Bitmap {
    if (width <= 0 || height <= 0) {
        return this
    }
    
    val size = Size(width.toFloat(), height.toFloat())
    val outline = shape.createOutline(size, layoutDirection, density)
    val target = ImageBitmap(width, height, ImageBitmapConfig.Argb8888)
    val source = asImageBitmap()
    
    CanvasDrawScope().draw(density, layoutDirection, GraphicsCanvas(target), size) {
        drawOutline(outline = outline, color = Color.Black)
        drawImage(image = source, blendMode = BlendMode.SrcIn)
    }
    
    return target.asAndroidBitmap()
}

private fun loadSampledBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        }
        
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = computeSampleSize(
                boundsOptions.outWidth, boundsOptions.outHeight, MAX_SOURCE_DIMENSION
            )
            
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
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

private val SNAP_SPEC = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow
)

private val MASK_INSET = 16.dp
private const val MAX_SCALE = 10f
private const val SCRIM_ALPHA = 0.62f
private const val MAX_SOURCE_DIMENSION = 2048
private const val FULL_TURN_STEPS = 4
