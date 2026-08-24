/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import android.net.Uri
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.max

/** Скругление, с которым содержимое садится обратно в миниатюру. */
private val MEDIA_ORIGIN_CORNER_RADIUS = 4.dp

private val OPEN_SPEC: AnimationSpec<Float> =
    tween(durationMillis = 260, easing = FastOutSlowInEasing)

private val CLOSE_SPEC: AnimationSpec<Float> =
    tween(durationMillis = 220, easing = FastOutSlowInEasing)

/**
 * Экранные границы миниатюр, из которых открывается полноэкранный просмотр.
 *
 * Границы хранятся в координатах экрана, а не окна, потому что миниатюра и
 * просмотрщик могут жить в разных окнах: сетка шторки вложений лежит в окне
 * шторки, а её предпросмотр — в своём собственном.
 *
 * Миниатюра забывается, как только уходит из композиции. Именно так переход
 * узнаёт, что возвращаться некуда: пролистанного за пределы экрана элемента в
 * списке уже нет.
 */
@Stable
class MediaOriginRegistry {
    
    private val bounds = mutableStateMapOf<String, Rect>()
    
    fun report(key: String, rect: Rect) {
        bounds[key] = rect
    }
    
    fun forget(key: String) {
        bounds.remove(key)
    }
    
    fun boundsOf(key: String?): Rect? = key?.let { bounds[it] }
}

private val GlobalMediaOriginRegistry = MediaOriginRegistry()

val LocalMediaOriginRegistry = staticCompositionLocalOf { GlobalMediaOriginRegistry }

/** Ключ миниатюры вложения в переписке. */
fun chatMediaKey(uri: Uri): String = uri.toString()

/**
 * Ключ миниатюры в сетке шторки вложений.
 *
 * Приставка нужна, чтобы одно и то же изображение в переписке и в галерее не
 * занимало одну запись: иначе закрытая шторка забрала бы с собой границы
 * миниатюры сообщения.
 */
fun pickerMediaKey(uri: Uri): String = "picker:$uri"

/**
 * Запоминает, где на экране лежит миниатюра [key], пока она видна.
 */
@Composable
fun Modifier.mediaTransitionOrigin(key: String): Modifier {
    val registry = LocalMediaOriginRegistry.current
    val view = LocalView.current
    
    DisposableEffect(registry, key) {
        onDispose { registry.forget(key) }
    }
    
    return this.onGloballyPositioned { coordinates ->
        registry.report(key, coordinates.boundsOnScreen(view))
    }
}

internal fun LayoutCoordinates.boundsOnScreen(view: View): Rect =
    boundsInWindow().translate(view.windowOffsetOnScreen())

/**
 * Где начинается окно [this] на экране.
 *
 * Разница между положением одного и того же места в окне и на экране и есть
 * сдвиг самого окна: свой способ спросить его напрямую есть не у каждого окна.
 */
private fun View.windowOffsetOnScreen(): Offset {
    val onScreen = IntArray(2)
    val inWindow = IntArray(2)
    
    getLocationOnScreen(onScreen)
    getLocationInWindow(inWindow)
    
    return Offset(
        x = (onScreen[0] - inWindow[0]).toFloat(), y = (onScreen[1] - inWindow[1]).toFloat()
    )
}

/** Куда и в каком виде рисуется содержимое просмотрщика прямо сейчас. */
internal data class MediaHeroFrame(
    val rect: Rect, val scale: Float, val cornerRadius: Float, val alpha: Float
)

/**
 * Переход содержимого между миниатюрой и полным экраном.
 *
 * Содержимое всегда занимает весь просмотрщик, а к миниатюре его приводят
 * масштаб, сдвиг и обрезка: так одна и та же анимация работает и с фотографией,
 * и с видео, и с гифкой, и переживает зум, потому что зум живёт внутри
 * содержимого и уезжает вместе с ним.
 *
 * Вертикальный свайп сюда же и передаётся: [dragOffsetY] входит в границы
 * открытого состояния, поэтому отпущенное на полпути содержимое уменьшается
 * именно оттуда, где его оставил палец, а не прыгает сначала в центр.
 *
 * Когда миниатюры на экране нет, возвращаться некуда, и содержимое просто
 * уезжает за край и тает вместе с фоном.
 */
@Stable
class MediaHeroState internal constructor(
    private val registry: MediaOriginRegistry,
    private val view: View,
    private val cornerRadiusPx: Float,
    private val scope: CoroutineScope
) {
    
    private val clipPath = Path()
    private val expansion = Animatable(0f)
    private val exit = Animatable(0f)
    
    internal var container by mutableStateOf(Rect.Zero)
    internal var dragOffsetY by mutableFloatStateOf(0f)
    internal var originKey by mutableStateOf<String?>(null)
    internal var onDismissed: () -> Unit = {}
    
    var isOpening by mutableStateOf(true)
        private set
    
    var isClosing by mutableStateOf(false)
        private set
    
    /** Просмотрщик стоит на месте: ни открывается, ни закрывается. */
    val isSettled: Boolean
        get() = !isOpening && !isClosing
    
    /** Насколько фон просмотрщика уже проявился: от нуля до единицы. */
    val backgroundFraction: Float
        get() = expansion.value * (1f - exit.value)
    
    private val origin: Rect?
        get() {
            val bounds = registry.boundsOf(originKey) ?: return null
            
            if (bounds.width <= 0f || bounds.height <= 0f) {
                return null
            }
            
            return if (bounds.overlaps(screenBounds)) bounds else null
        }
    
    private val screenBounds: Rect
        get() {
            val metrics = view.resources.displayMetrics
            
            return Rect(
                left = 0f,
                top = 0f,
                right = metrics.widthPixels.toFloat(),
                bottom = metrics.heightPixels.toFloat()
            )
        }
    
    internal suspend fun open() {
        isOpening = true
