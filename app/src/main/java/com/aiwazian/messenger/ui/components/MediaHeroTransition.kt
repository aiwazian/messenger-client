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
 * узнаёт, что возвращаться некуда: улетевшего за пределы экрана элемента списка
 * больше нет.
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

/** Запоминает, где на экране лежит миниатюра [key], пока она видна. */
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
 * Разница между положением одной и той же точки в окне и на экране и есть
 * сдвиг самого окна.
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
 * масштаб, сдвиг и обрезка. Поэтому одна и та же анимация работает и с фото,
 * и с видео, и с гифкой, и переживает зум: зум живёт внутри содержимого и
 * уезжает в миниатюру вместе с ним, без сброса.
 *
 * Вертикальный свайп сюда же и передаётся: [dragOffsetY] входит в границы
 * открытого состояния, и отпущенное на полпути содержимое уменьшается именно
 * оттуда, где его оставил палец, а не прыгает сначала в центр.
 *
 * Когда миниатюры на экране нет, возвращаться некуда, и содержимое просто
 * уезжает за край экрана и тает вместе с фоном.
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
        expansion.animateTo(1f, OPEN_SPEC)
        isOpening = false
    }
    
    /**
     * Закрывает просмотрщик и сообщает об этом владельцу, когда анимация доиграла.
     *
     * До этого момента просмотрщик обязан оставаться в композиции, иначе
     * анимировать будет нечего.
     */
    fun dismiss() {
        if (isClosing) {
            return
        }
        
        isClosing = true
        
        scope.launch {
            if (origin != null) {
                expansion.animateTo(0f, CLOSE_SPEC)
            } else {
                exit.animateTo(1f, CLOSE_SPEC)
            }
            
            onDismissed()
        }
    }
    
    internal fun frame(size: Size): MediaHeroFrame {
        val full = Rect(Offset.Zero, size)
        
        /*
         * Границы просмотрщика приходят только после разметки. До этого считать
         * нечего, а показать содержимое во весь экран — значит мигнуть им до того,
         * как оно выедет из миниатюры.
         */
        if (container.isEmpty || size.width <= 0f || size.height <= 0f) {
            return MediaHeroFrame(rect = full, scale = 1f, cornerRadius = 0f, alpha = 0f)
        }
        
        val progress = expansion.value
        val escaped = exit.value
        val dragged = full.translate(0f, dragOffsetY)
        val origin = origin
        
        if (origin == null) {
            /*
             * Уезжаем в ту же сторону, куда тянул палец, и вниз во всех остальных
             * случаях: разворот содержимого посреди жеста выглядит рывком.
             */
            val travel = if (dragOffsetY < 0f) {
                -(size.height + dragOffsetY)
            } else {
                size.height - dragOffsetY
            }
            
            return MediaHeroFrame(
                rect = dragged.translate(0f, escaped * travel),
                scale = 1f,
                cornerRadius = 0f,
                alpha = progress * (1f - escaped)
            )
        }
        
        val target = origin.translate(-container.left, -container.top)
        val rect = lerp(target, dragged, progress)
        
        return MediaHeroFrame(
            rect = rect,
            /*
             * Миниатюра обрезана по своему квадрату, а полный экран вписывает
             * картинку целиком. Больший из двух масштабов закрывает всю область
             * миниатюры, а лишнее срезает обрезка — тот же эффект, что у Crop.
             */
            scale = max(rect.width / size.width, rect.height / size.height),
            cornerRadius = lerp(cornerRadiusPx, 0f, progress),
            alpha = 1f
        )
    }
    
    internal fun clipPath(frame: MediaHeroFrame): Path = clipPath.apply {
        rewind()
        addRoundRect(RoundRect(frame.rect, CornerRadius(frame.cornerRadius)))
    }
}

/**
 * Создаёт переход и сразу разворачивает содержимое из миниатюры [originKey].
 *
 * @param originKey миниатюра того, что показано сейчас, а не того, с чего начали:
 * после перелистывания возвращаться надо в соседнюю миниатюру.
 * @param dragOffsetY текущее смещение от вертикального свайпа.
 * @param onDismissed вызывается, когда просмотрщик уже можно убирать с экрана.
 */
@Composable
fun rememberMediaHeroState(
    originKey: String?, dragOffsetY: Float, onDismissed: () -> Unit
): MediaHeroState {
    val registry = LocalMediaOriginRegistry.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val cornerRadiusPx = with(LocalDensity.current) { MEDIA_ORIGIN_CORNER_RADIUS.toPx() }
    val currentOnDismissed by rememberUpdatedState(onDismissed)
    
    val state = remember(registry, view, scope) {
        MediaHeroState(
            registry = registry, view = view, cornerRadiusPx = cornerRadiusPx, scope = scope
        )
    }
    
    state.originKey = originKey
    state.dragOffsetY = dragOffsetY
    state.onDismissed = { currentOnDismissed() }
    
    LaunchedEffect(state) { state.open() }
    
    return state
}

/** Область, относительно которой считаются границы перехода. */
@Composable
fun Modifier.mediaHeroContainer(state: MediaHeroState): Modifier {
    val view = LocalView.current
    
    return this.onGloballyPositioned { coordinates ->
        state.container = coordinates.boundsOnScreen(view)
    }
}

/**
 * Рисует содержимое там и таким, как требует текущий кадр перехода.
 *
 * Обрезка стоит снаружи преобразования, иначе она масштабировалась бы вместе с
 * содержимым и ничего не обрезала.
 */
fun Modifier.mediaHeroContent(state: MediaHeroState): Modifier = this
    .drawWithContent {
        val frame = state.frame(size)
        
        clipPath(state.clipPath(frame)) { this@drawWithContent.drawContent() }
    }
    .graphicsLayer {
        val frame = state.frame(size)
        
        scaleX = frame.scale
        scaleY = frame.scale
        translationX = frame.rect.center.x - size.width / 2f
        translationY = frame.rect.center.y - size.height / 2f
        alpha = frame.alpha
    }

/**
 * Заливает фон просмотрщика, гася его вместе с переходом.
 *
 * @param alpha непрозрачность, которую задаёт сам просмотрщик: обычно она уже
 * учитывает вертикальный свайп.
 */
fun Modifier.mediaHeroBackground(
    state: MediaHeroState, color: Color, alpha: () -> Float
): Modifier = this.drawBehind {
    drawRect(color = color, alpha = (alpha() * state.backgroundFraction).coerceIn(0f, 1f))
}
