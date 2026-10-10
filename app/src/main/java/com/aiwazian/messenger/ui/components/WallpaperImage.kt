/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import kotlin.math.max
import kotlin.math.roundToInt

private val MAX_WALLPAPER_BLUR_RADIUS = 30.dp

@Composable
fun WallpaperImage(
    model: Any?,
    darkenPercent: Int,
    blurPercent: Int,
    offsetX: Float,
    offsetY: Float,
    modifier: Modifier = Modifier,
    cacheKey: String? = null,
    onOffsetChange: ((Float, Float) -> Unit)? = null
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }

    val coverScale = if (containerSize.width > 0 && containerSize.height > 0 &&
        imageSize.width > 0 && imageSize.height > 0
    ) {
        max(
            containerSize.width / imageSize.width.toFloat(),
            containerSize.height / imageSize.height.toFloat()
        )
    } else {
        1f
    }

    val maxOffsetX = ((imageSize.width * coverScale - containerSize.width) / 2f).coerceAtLeast(0f)
    val maxOffsetY = ((imageSize.height * coverScale - containerSize.height) / 2f).coerceAtLeast(0f)

    val currentOffsetX by rememberUpdatedState(offsetX)
    val currentOffsetY by rememberUpdatedState(offsetY)
    val currentMaxOffsetX by rememberUpdatedState(maxOffsetX)
    val currentMaxOffsetY by rememberUpdatedState(maxOffsetY)
    val currentOnOffsetChange by rememberUpdatedState(onOffsetChange)

    Box(
        modifier = modifier
            .onSizeChanged { containerSize = it }
            .then(
                if (onOffsetChange != null) {
                    Modifier.pointerInput(Unit) {
                        var translationX = 0f
                        var translationY = 0f

                        detectDragGestures(
                            onDragStart = {
                                translationX = currentOffsetX * currentMaxOffsetX
                                translationY = currentOffsetY * currentMaxOffsetY
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()

                                translationX = (translationX + dragAmount.x)
                                    .coerceIn(-currentMaxOffsetX, currentMaxOffsetX)
                                translationY = (translationY + dragAmount.y)
                                    .coerceIn(-currentMaxOffsetY, currentMaxOffsetY)

                                val fractionX = if (currentMaxOffsetX > 0f) translationX / currentMaxOffsetX else 0f
                                val fractionY = if (currentMaxOffsetY > 0f) translationY / currentMaxOffsetY else 0f

                                currentOnOffsetChange?.invoke(fractionX, fractionY)
                            }
                        )
                    }
                } else {
                    Modifier
                }
            )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(model)
                .apply {
                    if (cacheKey != null) {
                        memoryCacheKey(cacheKey)
                        diskCacheKey(cacheKey)
                    }
                }
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            onSuccess = { state ->
                val size = state.painter.intrinsicSize

                if (size != Size.Unspecified && size.width > 0f && size.height > 0f) {
                    imageSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (blurPercent > 0) {
                        Modifier.blur(
                            radius = MAX_WALLPAPER_BLUR_RADIUS * (blurPercent.coerceIn(0, MAX_EFFECT_PERCENT) / MAX_EFFECT_PERCENT.toFloat()),
                            edgeTreatment = BlurredEdgeTreatment.Unbounded
                        )
                    } else {
                        Modifier
                    }
                )
                .graphicsLayer {
                    scaleX = coverScale
                    scaleY = coverScale
                    translationX = offsetX * maxOffsetX
                    translationY = offsetY * maxOffsetY
                }
        )

        if (darkenPercent > 0) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = darkenPercent.coerceIn(0, MAX_EFFECT_PERCENT) / 100f))
            )
        }
    }
}

internal const val MAX_EFFECT_PERCENT = 90
