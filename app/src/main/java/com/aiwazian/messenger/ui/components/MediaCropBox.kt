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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.aiwazian.messenger.utils.media.mirrored
import com.aiwazian.messenger.utils.media.rotatedQuarter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Кадрирование одной картинки под маску: панорама, масштаб, поворот, отражение.
 *
 * Состояние живёт отдельно от разметки, потому что подтверждение и правка кадра
 * нажимаются в чужой панели: у выбора медиа своя нижняя панель во весь экран, и
 * внутрь области кадрирования она не помещается.
 *
 * Геометрию записывает [MediaCropBox] при измерении — снаружи она неизвестна, а
 * без стороны маски и размеров окна вырезаемый прямоугольник не посчитать.
 */
class MediaCropState internal constructor() {
    
    /** Кадр в текущем виде: поворот и отражение применяются к самому битмапу. */
    var bitmap by mutableStateOf<Bitmap?>(null)
        private set
    
    internal var scale by mutableFloatStateOf(1f)
    
    internal var minScale by mutableFloatStateOf(1f)
        private set
    
    internal val offsetX = Animatable(0f)
    internal val offsetY = Animatable(0f)
    
    /** Множитель «вписать кадр в окно»: масштаб пользователя считается от него. */
    private var fitScale = 1f
    
    private var maskSide = 0f
    private var viewportWidth = 0f
    private var viewportHeight = 0f
    
    /** Кадр загружен и окно измерено: до этого вырезать нечего. */
    val isReady: Boolean
        get() = bitmap != null && maskSide > 0f
    
    internal suspend fun setBitmap(value: Bitmap?) {
        bitmap = value
        scale = 1f
        
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
    
    internal suspend fun pan(change: Offset) {
        offsetX.snapTo(offsetX.value + change.x)
        offsetY.snapTo(offsetY.value + change.y)
    }
    
    /**
     * Жест закончился — кадр подтягивается назад, пока маска не заполнена.
     *
     * Сдвиг ограничивается не во время жеста, а после него: упор в край посреди
     * перетаскивания читался бы как залипание пальца.
     */
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
    
    /**
     * Поворот на четверть: пересобирается сам битмап, а не слой отрисовки.
     *
     * Иначе вырезать пришлось бы из повёрнутой системы координат, и та же
     * математика понадобилась бы дважды — на экране и при сохранении.
     */
    suspend fun rotate() {
        val source = bitmap ?: return
        val previousFitScale = fitScale
        
        val rotated = source.rotatedQuarter()
        
        bitmap = rotated
        applyLimits(rotated)
        
        /*
         * Масштаб хранится множителем к вписанному размеру, а после поворота
         * вписывание другое: без пересчёта кадр прыгнул бы в размере.
         */
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
    }
    
    /**
     * Квадрат под маской в пикселях исходного кадра.
     *
     * Возвращается именно квадрат, даже когда маска круглая: прозрачные углы
     * рисует уже отправка, а промежуточный кадр остаётся обычной картинкой.
     */
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
    
    /**
     * Нижний предел масштаба — тот, при котором маска ещё заполнена целиком.
     *
     * Пустоту под маской не даём выбрать вовсе: у аватарки она стала бы дырой в
     * кружке, а у стикера — незаметной на превью прозрачной полосой.
     */
    private fun applyLimits(source: Bitmap) {
        if (viewportWidth <= 0f || viewportHeight <= 0f || maskSide <= 0f) {
            return
        }
        
        fitScale = minOf(viewportWidth / source.width, viewportHeight / source.height)
        
        minScale = (maskSide / minOf(
            source.width * fitScale, source.height * fitScale
        )).coerceAtLeast(1f)
        
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

/**
 * Кадр под затемнением с вырезом заданной формы.
 *
 * Форма приходит снаружи: у аватарки вырез круглый, у стикера — квадрат со
 * скруглением темы. Кроме формы эти два случая ничем не отличаются, поэтому
 * второго экрана кадрирования нет.
 *
 * @param contentRotation поворот, который отыгрывает панель правки, пока новый
 * битмап ещё не подставлен.
 * @param contentScaleX отражение по горизонтали из той же панели.
 * @param isGestureEnabled на время анимации правки жесты выключаются: масштаб,
 * посчитанный от кадра в движении, разъезжается с итоговым.
 */
@Composable
fun MediaCropBox(
    state: MediaCropState,
    maskShape: Shape,
    modifier: Modifier = Modifier,
    contentRotation: Float = 0f,
    contentScaleX: Float = 1f,
    isGestureEnabled: Boolean = true
) {
    val coroutineScope = rememberCoroutineScope()
    
    val transformableState = rememberTransformableState { _, zoomChange, panChange, _ ->
        if (!isGestureEnabled) return@rememberTransformableState
        
        state.scale = (state.scale * zoomChange).coerceIn(state.minScale, MAX_SCALE)
        
        coroutineScope.launch { state.pan(panChange) }
    }
    
    BoxWithConstraints(modifier = modifier.transformable(state = transformableState)) {
        val inset = with(LocalDensity.current) { MASK_INSET.toPx() }
        
        val viewportWidth = constraints.maxWidth.toFloat()
        val viewportHeight = constraints.maxHeight.toFloat()
        
        val side = (minOf(viewportWidth, viewportHeight) - inset * 2f).coerceAtLeast(1f)
        
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
        
        val scrimColor = MaterialTheme.colorScheme.surface.copy(alpha = SCRIM_ALPHA)
        val layoutDirection = LocalLayoutDirection.current
        
        /*
         * Затемнение и вырез — один слой: BlendMode.Clear стирает уже
         * нарисованное, поэтому вырез обязан попасть в тот же offscreen-слой,
         * иначе он выест и кадр под затемнением.
         */
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
            drawRect(color = scrimColor)
            
            val outline = maskShape.createOutline(
                size = Size(side, side), layoutDirection = layoutDirection, density = this
            )
            
            translate(left = (size.width - side) / 2f, top = (size.height - side) / 2f) {
                drawOutline(
                    outline = outline, color = Color.Transparent, blendMode = BlendMode.Clear
                )
            }
        }
    }
}

/**
 * Кадр читается уменьшенным: кадрирование правит рамку, а не пиксели, и полный
 * снимок современной камеры занял бы в памяти десятки мегабайт впустую.
 */
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
