/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.aiwazian.messenger.utils.media.VideoCropRect
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.min

@Stable
class VideoCropState internal constructor() {

    var sourceWidth by mutableIntStateOf(0)
        private set

    var sourceHeight by mutableIntStateOf(0)
        private set

    var displayWidth by mutableIntStateOf(0)
        private set

    var displayHeight by mutableIntStateOf(0)
        private set

    var fitScale by mutableFloatStateOf(0f)
        private set

    internal var scale by mutableFloatStateOf(1f)

    internal var minScale by mutableFloatStateOf(1f)
        private set

    internal val offsetX = Animatable(0f)
    internal val offsetY = Animatable(0f)

    private var maskSide = 0f
    private var viewportWidth = 0f
    private var viewportHeight = 0f
    private var quarterTurns = 0

    val isReady: Boolean
        get() = sourceWidth > 0 && maskSide > 0f

    internal fun onMeasured(width: Float, height: Float, side: Float) {
        viewportWidth = width
        viewportHeight = height
        maskSide = side

        applyLimits()
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
        if (displayWidth <= 0 || displayHeight <= 0) {
            return@coroutineScope
        }

        val halfWidth = displayWidth * fitScale * scale / 2f
        val halfHeight = displayHeight * fitScale * scale / 2f

        val maxOffsetX = (halfWidth - maskSide / 2f).coerceAtLeast(0f)
        val maxOffsetY = (halfHeight - maskSide / 2f).coerceAtLeast(0f)

        launch {
            offsetX.animateTo(offsetX.value.coerceIn(-maxOffsetX, maxOffsetX), SNAP_SPEC)
        }

        launch {
            offsetY.animateTo(offsetY.value.coerceIn(-maxOffsetY, maxOffsetY), SNAP_SPEC)
        }
    }

    suspend fun applyOrientation(newQuarterTurns: Int) {
        quarterTurns = newQuarterTurns

        applyLimits()

        coroutineScope {
            launch { offsetX.animateTo(0f, SNAP_SPEC) }
            launch { offsetY.animateTo(0f, SNAP_SPEC) }
        }
    }

    suspend fun setSourceDimensions(width: Int, height: Int) {
        sourceWidth = width
        sourceHeight = height
        scale = 1f
        quarterTurns = 0

        applyLimits()

        offsetX.snapTo(0f)
        offsetY.snapTo(0f)
    }

    private fun applyLimits() {
        val swapped = quarterTurns % 2 != 0

        displayWidth = if (swapped) sourceHeight else sourceWidth
        displayHeight = if (swapped) sourceWidth else sourceHeight

        if (viewportWidth <= 0f || viewportHeight <= 0f || maskSide <= 0f) {
            return
        }

        if (displayWidth <= 0 || displayHeight <= 0) {
            return
        }

        fitScale = min(viewportWidth / displayWidth, viewportHeight / displayHeight)

        val shortSide = min(displayWidth * fitScale, displayHeight * fitScale)

        minScale = if (shortSide > 0f) maskSide / shortSide else 1f

        if (scale < minScale) {
            scale = minScale
        }
    }

    fun cropRect(): VideoCropRect? {
        if (maskSide <= 0f || displayWidth <= 0 || displayHeight <= 0) {
            return null
        }

        val totalScale = fitScale * scale

        if (totalScale <= 0f) {
            return null
        }

        val halfSide = maskSide / 2f
        val width = displayWidth.toFloat()
        val height = displayHeight.toFloat()

        val leftPx = (width / 2f + (-halfSide - offsetX.value) / totalScale)
            .coerceIn(0f, width)
        val rightPx = (width / 2f + (halfSide - offsetX.value) / totalScale)
            .coerceIn(0f, width)
        val topPx = (height / 2f + (-halfSide - offsetY.value) / totalScale)
            .coerceIn(0f, height)
        val bottomPx = (height / 2f + (halfSide - offsetY.value) / totalScale)
            .coerceIn(0f, height)

        if (rightPx - leftPx < 1f || bottomPx - topPx < 1f) {
            return null
        }

        return VideoCropRect(
            left = leftPx / width,
            top = topPx / height,
            right = rightPx / width,
            bottom = bottomPx / height
        )
    }
}

@Composable
fun rememberVideoCropState(): VideoCropState = remember { VideoCropState() }

private val SNAP_SPEC = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow
)

private const val MAX_SCALE = 10f
