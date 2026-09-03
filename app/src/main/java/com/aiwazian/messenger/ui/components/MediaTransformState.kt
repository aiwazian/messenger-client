/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.Rotate90DegreesCcw
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import com.aiwazian.messenger.utils.media.MediaTransform
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Поворот и отражение с отыгранной анимацией — общая часть экрана обрезки
 * аватарки и предпросмотра вложений.
 *
 * Куда повернут кадр, знает [transform]. Сами же градусы на экране живут
 * отдельно, в [Animatable]: поворот обязан доезжать, а не перескакивать на
 * четверть сразу.
 *
 * Два режима различаются тем, что становится истиной после шага.
 * Аватарка пересобирает битмап — там поворот уходит в пиксели, а слой
 * возвращается в ноль. Предпросмотр вложения не пересобирает ничего:
 * битмапа у видео нет, а перекодировать его ради предпросмотра никто не
 * станет — там поворот остаётся в слое до самой отправки.
 */
@Stable
class MediaTransformState internal constructor(
    initial: MediaTransform,
    private val bakesContent: Boolean
) {
    /** Положение кадра после всех доехавших шагов. */
    var transform by mutableStateOf(initial)
        private set
    
    /** Идёт анимация: чужие жесты и новые нажатия на это время лишние. */
    var isAnimating by mutableStateOf(false)
        private set
    
    /** Есть что сбрасывать: именно по этому появляется «Сбросить». */
    val isChanged: Boolean
        get() = !transform.isIdentity
    
    /** Градусы для слоя: положение анимации, а не итог шага. */
    val contentRotation: Float
        get() = rotation.value
    
    /**
     * Множитель по горизонтали для слоя.
     *
     * В режиме пересборки отражение уже в пикселях, и слой обязан вернуться
     * к единице — иначе отражённый кадр отразился бы второй раз и вернулся к
     * исходному виду.
     */
    val contentScaleX: Float
        get() = if (bakesContent) flipScale.value else flipScale.value * transform.mirrorScaleX
    
    private val rotation = Animatable(initial.rotationDegrees.toFloat())
    private val flipScale = Animatable(1f)
    
    /**
     * Ещё четверть против часовой.
     *
     * @param onApplied вызывается, когда поворот доехал и попал в [transform]:
     * в режиме пересборки здесь меняют сам кадр. Слой возвращается в ноль
     * сразу после него — в том же кадре отрисовки, поэтому исходное
     * положение мигнуть не успевает.
     */
    suspend fun rotate(onApplied: suspend (MediaTransform) -> Unit = {}) {
        if (isAnimating) {
            return
        }
        
        isAnimating = true
        
        try {
            rotation.animateTo(
                targetValue = rotation.value - MediaTransform.QUARTER_TURN,
                animationSpec = rotationSpec()
            )
            
            transform = transform.rotated()
            onApplied(transform)
            
            if (bakesContent) {
                rotation.snapTo(0f)
            }
        } finally {
            isAnimating = false
        }
    }
    
    /**
     * Отражение по горизонтали.
     *
     * Кадр схлопывается до нулевой ширины и раскрывается уже отражённым:
     * мгновенная подмена выглядела бы рывком. Заодно в этот момент видно
     * ровно ничего, и сюда же уезжает подмена градусов, которую требует
     * экранное отражение повёрнутого кадра.
     */
    suspend fun flip(onApplied: suspend (MediaTransform) -> Unit = {}) {
        if (isAnimating) {
            return
        }
        
        isAnimating = true
        
        try {
            flipScale.animateTo(targetValue = 0f, animationSpec = flipSpec())
            
            transform = transform.mirrored()
            onApplied(transform)
            
            if (!bakesContent) {
                rotation.snapTo(-rotation.value)
            }
            
            flipScale.animateTo(targetValue = 1f, animationSpec = flipSpec())
        } finally {
            isAnimating = false
        }
    }
    
    /**
     * Возвращает кадру исходное положение.
     *
     * Поворот доезжает к ближайшему нулю, а не к абсолютному: с трёх четвертей
     * короче довернуть четвёртую, чем открутить три назад.
     */
    suspend fun reset(onApplied: suspend (MediaTransform) -> Unit = {}) {
        if (isAnimating || !isChanged) {
            return
        }
        
        isAnimating = true
        
        try {
            val wasMirrored = transform.isMirrored
            
            if (wasMirrored) {
                flipScale.animateTo(targetValue = 0f, animationSpec = flipSpec())
            }
            
            transform = MediaTransform.None
            onApplied(MediaTransform.None)
            
            if (bakesContent) {
                rotation.snapTo(0f)
                flipScale.snapTo(1f)
                return
            }
            
            coroutineScope {
                if (wasMirrored) {
                    launch { flipScale.animateTo(1f, flipSpec()) }
                }
                
                launch { rotation.animateTo(nearestZeroTurn(rotation.value), rotationSpec()) }
            }
        } finally {
            isAnimating = false
        }
    }
    
    private fun rotationSpec(): AnimationSpec<Float> = tween(
        durationMillis = ROTATE_DURATION_MS, easing = FastOutSlowInEasing
    )
    
    private fun flipSpec(): AnimationSpec<Float> = tween(
        durationMillis = FLIP_DURATION_MS, easing = FastOutSlowInEasing
    )
    
    private companion object {
        const val ROTATE_DURATION_MS = 300
        const val FLIP_DURATION_MS = 150
    }
}

/**
 * @param initial положение, с которого начинается правка: вложение могли
 * повернуть и раньше, а предпросмотр открывают по второму разу.
 * @param bakesContent пересобирается ли сам кадр после каждого шага.
 * @param key чьё положение правим: с его сменой правка начинается заново.
 */
@Composable
fun rememberMediaTransformState(
    initial: MediaTransform = MediaTransform.None,
    bakesContent: Boolean = false,
    key: Any? = Unit
): MediaTransformState = remember(key) { MediaTransformState(initial, bakesContent) }

/**
 * Слой, который держит повёрнутый кадр в своих границах.
 *
 * Поворот на четверть меняет стороны местами, и горизонтальная фотография,
 * повёрнутая как есть, вылезла бы за края экрана. Поэтому вместе с поворотом
 * кадр уменьшается — ровно настолько, чтобы вписаться, и не рывком в конце,
 * а вместе с промежуточными градусами.
 *
 * @param contentSize размеры самого кадра. Нулевые означают «ещё не
 * загрузился»: вписывать нечего, и слой только поворачивает.
 */
fun Modifier.mediaTransform(state: MediaTransformState, contentSize: Size): Modifier =
    this.graphicsLayer {
        val degrees = state.contentRotation
        val fit = fitScaleWhileRotating(size, contentSize, degrees)
        
        rotationZ = degrees
        scaleX = fit * state.contentScaleX
        scaleY = fit
    }

/** Кнопка отражения: одна и та же и у аватарки, и у вложения. */
@Composable
fun MediaFlipButton(
    state: MediaTransformState,
    modifier: Modifier = Modifier,
    onApplied: suspend (MediaTransform) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    
    IconButton(onClick = { scope.launch { state.flip(onApplied) } }, modifier = modifier) {
        Icon(imageVector = Icons.Rounded.Flip, contentDescription = null)
    }
}

/** Кнопка поворота на четверть против часовой. */
@Composable
fun MediaRotateButton(
    state: MediaTransformState,
    modifier: Modifier = Modifier,
    onApplied: suspend (MediaTransform) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    
    IconButton(onClick = { scope.launch { state.rotate(onApplied) } }, modifier = modifier) {
        Icon(imageVector = Icons.Rounded.Rotate90DegreesCcw, contentDescription = null)
    }
}

/**
 * Во сколько раз уменьшить кадр, чтобы повёрнутый на [degrees] остался в
 * границах [container].
 *
 * Считается от того размера, в котором кадр уже лежит без поворота, поэтому
 * на нуле градусов выходит ровно единица. Увеличивать не предлагается
 * никогда: предпросмотр показывает кадр, а не растягивает его по экрану.
 */
private fun fitScaleWhileRotating(container: Size, content: Size, degrees: Float): Float {
    if (container.width <= 0f || container.height <= 0f) {
        return 1f
    }
    
    if (content.width <= 0f || content.height <= 0f) {
        return 1f
    }
    
    val fit = min(container.width / content.width, container.height / content.height)
    val width = content.width * fit
    val height = content.height * fit
    
    val radians = degrees * PI / STRAIGHT_ANGLE
    val cos = abs(cos(radians)).toFloat()
    val sin = abs(sin(radians)).toFloat()
    
    // Габариты повёрнутой рамки: так считается любая промежуточная четверть.
    val rotatedWidth = width * cos + height * sin
    val rotatedHeight = width * sin + height * cos
    
    if (rotatedWidth <= 0f || rotatedHeight <= 0f) {
        return 1f
    }
    
    return min(container.width / rotatedWidth, container.height / rotatedHeight)
        .coerceAtMost(1f)
}

/** Ближайшие градусы, на которых кадр стоит прямо. */
private fun nearestZeroTurn(degrees: Float): Float =
    (degrees / FULL_TURN).roundToInt() * FULL_TURN.toFloat()

private const val FULL_TURN = 360
private const val STRAIGHT_ANGLE = 180
