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
 * Пропуска