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

private val MEDIA_ORIGIN_CORNER_RADIUS = 4.dp

private val OPEN_SPEC: AnimationSpec<Float> =
    tween(durationMillis = 260, easing = FastOutSlowInEasing)

private val CLOSE_SPEC: AnimationSpec<Float> =
    tween(durationMillis = 220, easing = FastOutSlowInEasing)

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

fun chatMediaKey(messageId: Long, uri: Uri, scope: String = "chat"): String =
    "$scope:$messageId:$uri"

fun pickerMediaKey(uri: Uri): String = "picker:$uri"

@Composable
fun Modifier.mediaTransitionOrigin(key: String): Modifier =
    this.mediaTransitionBounds(key).mediaTransitionVisibility(key)

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

private fun View.windowOffsetOnScreen(): Offset {
    val onScreen = IntArray(2)
    val inWindow = IntArray(2)
    
    getLocationOnScreen(onScreen)
    getLocationInWindow(inWindow)
    
    return Offset(
        x = (onScreen[0] - inWindow[0]).toFloat(), y = (onScreen[1] - inWindow[1]).toFloat()
    )
}

internal data class MediaHeroFrame(
    val rect: Rect, val clip: Rect, val scale: Float, val cornerRadius: Float, val alpha: Float
)

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
    
    val isSettled: Boolean
        get() = !isOpening && !isClosing

    internal val isReady: Boolean
        get() = !container.isEmpty
    
    val backgroundFraction: Float
        get() = expansion.value * (1f - exit.value)
    
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
            clip = if (progress >= 1f) viewport else rect,
            scale = max(rect.width / content.width, rect.height / content.height),
            cornerRadius = lerp(cornerRadiusPx, 0f, progress),
            alpha = 1f
        )
    }
    
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

@Composable
fun Modifier.mediaHeroContainer(state: MediaHeroState): Modifier {
    val view = LocalView.current
    
    return this.onGloballyPositioned { coordinates ->
        state.container = coordinates.boundsOnScreen(view)
    }
}

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

fun Modifier.mediaHeroBackground(
    state: MediaHeroState, color: Color, alpha: () -> Float
): Modifier = this.drawBehind {
    drawRect(color = color, alpha = (alpha() * state.backgroundFraction).coerceIn(0f, 1f))
}

private fun Size.isUsable(): Boolean =
    width.isFinite() && height.isFinite() && width > 0f && height > 0f
