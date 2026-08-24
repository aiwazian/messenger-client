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
import kotlin.math.min

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
 *
 * Здесь же отмечается, какие миниатюры сейчас прятать: пока содержимое поднято
 * на весь экран, второй его копии в списке быть не должно.
 */
@Stable
class MediaOriginRegistry {
    
    private val bounds = mutableStateMapOf<String, Rect>()
    
    private val hidden = mutableStateMapOf<String, Int>()
    
    fun report(key: String, rect: Rect) {
        bounds[key] = rect
    }
    
    fun forget(key: String) {
        bounds.remove(key)
    }
    
    fun boundsOf(key: String?): Rect? = key?.let { bounds[it] }
    
    /**
     * Просит спрятать миниатюру [key] до парного [show].
     *
     * Счётчик, а не флаг: одно и то же вложение может встретиться в переписке
     * дважды, и вернуть миниатюру вправе только последний отпустивший.
     */
    internal fun hide(key: String) {
        hidden[key] = (hidden[key] ?: 0) + 1
    }
    
    internal fun show(key: String) {
        val rest = (hidden[key] ?: 0) - 1
        
        if (rest > 0) {
            hidden[key] = rest
        } else {
            hidden.remove(key)
        }
    }
    
    internal fun isHidden(key: String): Boolean = hidden.containsKey(key)
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
 * Запоминает, где на экране лежит миниатюра [key], и прячет её на время просмотра.
 *
 * Годится там, где миниатюра и есть весь элемент. Когда прятать нужно больше,
 * чем измерять, — например, всю ячейку сетки, а границы брать по уменьшенной
 * рамке внутри неё — берутся отдельные [mediaTransitionBounds] и
 * [mediaTransitionVisibility].
 */
@Composable
fun Modifier.mediaTransitionOrigin(key: String): Modifier =
    this.mediaTransitionBounds(key).mediaTransitionVisibility(key)

/** Запоминает, где на экране лежит миниатюра [key], пока она видна. */
@Composable
fun Modifier.mediaTransitionBounds(key: String): Modifier {
    val registry = LocalMediaOriginRegistry.current
    val view = LocalView.current
    
    DisposableEffect(registry, key) {
        onDispose { registry.forget(key) }
    }
    
    return this.onGloballyPositioned { coordinates ->
        registry.report(key, coordinates.boundsOnScreen(view))
    }
}

/**
 * Прячет содержимое, пока медиа [key] показывают во весь экран.
 *
 * Пропускается только отрисовка. Разметка остаётся на месте: спрятанная
 * миниатюра обязана и дальше сообщать свои границы, иначе возвращаться будет
 * некуда, и содержимое уедет за край экрана вместо неё.
 */
@Composable
fun Modifier.mediaTransitionVisibility(key: String): Modifier {
    val registry = LocalMediaOriginRegistry.current
    
    return this.drawWithContent {
        if (!registry.isHidden(key)) {
            drawContent()
        }
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

/**
 * Куда и в каком виде рисуется содержимое просмотрщика прямо сейчас.
 *
 * [rect] задаёт положение и размер содержимого, [clip] — окно, сквозь которое его
 * видно. Обычно это один и тот же прямоугольник, и расходятся они только в
 * раскрытом состоянии, где обрезать нечего.
 */
internal data class MediaHeroFrame(
    val rect: Rect, val clip: Rect, val scale: Float, val cornerRadius: Float, val alpha: Float
)

/**
 * Переход содержимого между миниатюрой и полным экраном.
 *
 * Содержимое всегда занимает весь просмотрщик, а к миниатюре его приводят
 * масштаб, сдвиг и обрезка. Поэтому одна и та же анимация работает и с фото,
 * и с видео, и с гифкой, и переживает зум: зум живёт внутри содержимого и
 * уезжает в миниатюру вместе с ним, без сброса.
 *
 * Считается всё от рамки самого содержимого, а не от границ просмотрщика.
 * Пустые поля сверху и снизу ничего не рисуют, зато растягивают путь обрезки:
 * пока прямоугольник едет от полного экрана, высота падает в разы быстрее
 * ширины, и квадратный кадр остаётся квадратом почти до самого конца, а потом
 * рывком поджимается в вытянутую миниатюру. От рамки содержимого обрезка идёт
 * вровень с уменьшением и сходит на нет ровно в конце.
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
    
    private var contentSize by mutableStateOf(Size.Zero)
    
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
    
    /**
     * Просмотрщик разметил себя и знает, куда рисовать содержимое.
     *
     * До этого момента прятать миниатюру рано: предпросмотр галереи живёт в
     * своём окне, и опустевшая ячейка успела бы мигнуть раньше первого кадра
     * перехода.
     */
    internal val isReady: Boolean
        get() = !container.isEmpty
    
    /** Насколько фон просмотрщика уже проявился: от нуля до единицы. */
    val backgroundFraction: Float
        get() = expansion.value * (1f - exit.value)
    
    /**
     * Сообщает собственные пропорции того, что показано сейчас.
     *
     * Единица измерения неважна, берётся только отношение сторон, поэтому годится
     * и размер картинки, и размер кадра видео.
     *
     * Нулевой размер означает «неизвестно»: тогда рамкой содержимого считается
     * весь просмотрщик. Так же сообщают и про увеличенное содержимое — оно выходит
     * за свою рамку, и обрезать по ней значило бы срезать полкадра в первый же
     * момент закрытия.
     */
    fun updateContentSize(size: Size) {
        contentSize = if (size.isUsable()) size else Size.Zero
    }
    
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
            return MediaHeroFrame(
                rect = full, clip = full, scale = 1f, cornerRadius = 0f, alpha = 0f
            )
        }
        
        val progress = expansion.value
        val escaped = exit.value
        val viewport = full.translate(0f, dragOffsetY)
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
            
            val escapedRect = viewport.translate(0f, escaped * travel)
            
            return MediaHeroFrame(
                rect = escapedRect,
                clip = escapedRect,
                scale = 1f,
                cornerRadius = 0f,
                alpha = progress * (1f - escaped)
            )
        }
        
        val content = contentRect(size)
        val target = origin.translate(-container.left, -container.top)
        val rect = lerp(target, content.translate(0f, dragOffsetY), progress)
        
        return MediaHeroFrame(
            rect = rect,
            /*
             * Раскрытое содержимое не обрезается вовсе: своей рамкой оно срезало бы
             * и соседние страницы листалки, которые заезжают сбоку со своими
             * пропорциями.
             */
            clip = if (progress >= 1f) viewport else rect,
            /*
             * Больший из двух масштабов закрывает всю область обрезки, а лишнее
             * срезается — тот же эффект, что у Crop в миниатюре. Считается он от
             * рамки содержимого, поэтому в раскрытом состоянии равен единице.
             */
            scale = max(rect.width / content.width, rect.height / content.height),
            cornerRadius = lerp(cornerRadiusPx, 0f, progress),
            alpha = 1f
        )
    }
    
    /**
     * Рамка, которую содержимое занимает внутри просмотрщика размером [size].
     *
     * Просмотрщики рисуют с ContentScale.Fit, поэтому содержимое сохраняет свои
     * пропорции и одна из сторон оказывается короче просмотрщика. Пока пропорции
     * неизвестны, рамкой считается весь просмотрщик: это размер содержимого,
     * которое заняло бы его целиком.
     */
    private fun contentRect(size: Size): Rect {
        val content = contentSize
        
        if (!content.isUsable()) {
            return Rect(Offset.Zero, size)
        }
        
        val fitScale = min(size.width / content.width, size.height / content.height)
        val width = content.width * fitScale
        val height = content.height * fitScale
        
        return Rect(
            offset = Offset((size.width - width) / 2f, (size.height - height) / 2f),
            size = Size(width, height)
        )
    }
    
    internal fun clipPath(frame: MediaHeroFrame): Path = clipPath.apply {
        rewind()
        addRoundRect(RoundRect(frame.clip, CornerRadius(frame.cornerRadius)))
    }
}

/**
 * Создаёт переход и сразу разворачивает содержимое из миниатюры [originKey].
 *
 * Пока просмотрщик на экране, миниатюра [originKey] спрятана: иначе рядом с
 * поднятым содержимым остаётся его копия, и переход выглядит подменой, а не
 * переносом. Возвращается миниатюра только вместе с уходом просмотрщика из
 * композиции, то есть уже после доигравшей анимации.
 *
 * @param originKey миниатюра того, что показано сейчас, а не того, с чего начали:
 * после перелистывания возвращаться надо в соседнюю миниатюру, а предыдущая
 * должна снова проявиться.
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
    
    val hiddenKey = originKey.takeIf { state.isReady }
    
    DisposableEffect(registry, hiddenKey) {
        hiddenKey?.let(registry::hide)
        
        onDispose { hiddenKey?.let(registry::show) }
    }
    
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

private fun Size.isUsable(): Boolean =
    width.isFinite() && height.isFinite() && width > 0f && height > 0f
