/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private class UndoEntry(val rect: Rect, val pixels: Bitmap)

@Stable
class MediaDrawRaster(val width: Int, val height: Int) {

    val bitmap: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    var hasInk by mutableStateOf(false)
        private set

    var undoCount by mutableIntStateOf(0)
        private set

    var revision by mutableIntStateOf(0)
        private set

    private val canvas = AndroidCanvas(bitmap)
    val overlay: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    private val overlayCanvas = AndroidCanvas(overlay)
    private val undoStack = ArrayDeque<UndoEntry>()

    private val brushPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = BRUSH_WIDTH_FRACTION * width
    }

    private val eraseCompositePaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
    }

    private val restorePaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
    }

    private var isErasingGesture = false
    private var lastPoint: Offset? = null
    private var gestureBounds: RectF? = null

    fun beginStroke(point: Offset, color: Color) {
        isErasingGesture = false
        clearOverlay()
        brushPaint.color = color.toArgb()
        lastPoint = point
        stamp(point)
    }

    fun continueStroke(point: Offset) {
        val from = lastPoint ?: return
        lastPoint = point
        overlayCanvas.drawLine(from.x, from.y, point.x, point.y, brushPaint)
        expandBounds(point)
        revision++
    }

    fun endStroke() {
        commit()
    }

    fun cancelPendingStroke() {
        clearOverlay()
    }

    fun beginErase() {
        isErasingGesture = true
        clearOverlay()
    }

    fun eraseAt(point: Offset) {
        val from = lastPoint
        lastPoint = point

        if (from == null) {
            stamp(point)
        } else {
            overlayCanvas.drawLine(from.x, from.y, point.x, point.y, brushPaint)
            expandBounds(point)
            revision++
        }
    }

    fun eraseTap(point: Offset) {
        beginErase()
        stamp(point)
        commit()
    }

    fun endErase() {
        commit()
    }

    fun undo() {
        val entry = undoStack.removeLastOrNull() ?: return

        canvas.drawBitmap(entry.pixels, null, RectF(entry.rect), restorePaint)
        undoCount = undoStack.size
        hasInk = true
        revision++
    }

    fun clearAll() {
        bitmap.eraseColor(TRANSPARENT)
        clearOverlay()
        undoStack.clear()
        undoCount = 0
        hasInk = false
        revision++
    }

    fun copy(): MediaDrawRaster {
        val copy = MediaDrawRaster(width, height)

        copy.canvas.drawBitmap(bitmap, 0f, 0f, null)
        copy.hasInk = hasInk
        undoStack.forEach { entry -> copy.undoStack.addLast(entry) }
        copy.undoCount = undoStack.size
        copy.revision++

        return copy
    }

    private fun stamp(point: Offset) {
        overlayCanvas.drawPoint(point.x, point.y, brushPaint)
        expandBounds(point)
        revision++
    }

    private fun expandBounds(point: Offset) {
        val margin = brushPaint.strokeWidth / 2f + BOUNDS_MARGIN_PX
        val bounds = gestureBounds ?: RectF(
            point.x - margin, point.y - margin, point.x + margin, point.y + margin
        ).also { gestureBounds = it }

        bounds.union(point.x - margin, point.y - margin)
        bounds.union(point.x + margin, point.y + margin)
    }

    private fun commit() {
        val bounds = gestureBounds

        if (bounds != null) {
            val left = max(0, bounds.left.roundToInt())
            val top = max(0, bounds.top.roundToInt())
            val right = min(width, bounds.right.roundToInt() + 1)
            val bottom = min(height, bounds.bottom.roundToInt() + 1)

            if (right > left && bottom > top) {
                val pixels = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)

                canvas.drawBitmap(
                    overlay, 0f, 0f, if (isErasingGesture) eraseCompositePaint else null
                )

                undoStack.addLast(UndoEntry(Rect(left, top, right, bottom), pixels))

                if (undoStack.size > MAX_UNDO_ENTRIES) {
                    undoStack.removeFirst()
                }

                undoCount = undoStack.size

                if (!isErasingGesture) {
                    hasInk = true
                }
            }
        }

        clearOverlay()
    }

    private fun clearOverlay() {
        overlay.eraseColor(TRANSPARENT)
        gestureBounds = null
        lastPoint = null
        revision++
    }

    companion object {
        const val MAX_DIMENSION = 2048
        private const val MAX_UNDO_ENTRIES = 30
        private const val BRUSH_WIDTH_FRACTION = 0.01f
        private const val BOUNDS_MARGIN_PX = 2f
        private const val TRANSPARENT = 0

        fun fromContentSize(size: androidx.compose.ui.geometry.Size): MediaDrawRaster {
            val scale = min(1f, MAX_DIMENSION / max(size.width, size.height).coerceAtLeast(1f))

            return MediaDrawRaster(
                width = (size.width * scale).roundToInt().coerceAtLeast(1),
                height = (size.height * scale).roundToInt().coerceAtLeast(1)
            )
        }
    }
}

@Composable
fun MediaDrawToolsRow(
    color: Color,
    isEraser: Boolean,
    onColorClick: () -> Unit,
    onEraserSelected: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = CircleShape
                )
                .clickable(onClick = onColorClick)
        )

        Spacer(modifier = Modifier.width(16.dp))

        IconButton(onClick = { onEraserSelected(false) }) {
            Icon(
                imageVector = Icons.Outlined.Draw,
                contentDescription = null,
                tint = if (!isEraser) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        IconButton(onClick = { onEraserSelected(true) }) {
            Icon(
                imageVector = Icons.Outlined.CleaningServices,
                contentDescription = null,
                tint = if (isEraser) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
fun MediaDrawLayer(
    raster: MediaDrawRaster,
    color: Color,
    isEraser: Boolean,
    modifier: Modifier = Modifier,
    zoomableState: ZoomableState? = null,
    zoomCoordinateOffset: Offset = Offset.Zero
) {
    val currentColor by rememberUpdatedState(color)
    val currentIsEraser by rememberUpdatedState(isEraser)
    val currentZoomableState by rememberUpdatedState(zoomableState)
    val currentZoomOffset by rememberUpdatedState(zoomCoordinateOffset)
    val gestureScope = rememberCoroutineScope()
    val rasterImage = remember(raster) { raster.bitmap.asImageBitmap() }
    val overlayImage = remember(raster) { raster.overlay.asImageBitmap() }

    Canvas(
        modifier = modifier
            .clipToBounds()
            .graphicsLayer {
                compositingStrategy = if (currentIsEraser) {
                    CompositingStrategy.Offscreen
                } else {
                    CompositingStrategy.Auto
                }
            }
            .pointerInput(raster, isEraser) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downPoint = down.position.toRaster(raster, size)
                    var isZooming = false
                    var didMove = false

                    if (isEraser) {
                        raster.beginErase()
                    } else {
                        raster.beginStroke(downPoint, currentColor)
                    }

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressedCount = event.changes.count { it.pressed }

                        if (pressedCount >= 2) {
                            if (!isZooming) {
                                isZooming = true
                                if (isEraser) raster.endErase() else raster.cancelPendingStroke()
                            }

                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val centroid = event.calculateCentroid(useCurrent = true)

                            if (!centroid.isUnspecified && (zoom != 1f || pan != Offset.Zero)) {
                                currentZoomableState?.applyTransform(
                                    centroid = centroid + currentZoomOffset,
                                    pan = pan,
                                    zoom = zoom
                                )
                            }

                            event.changes.forEach { change ->
                                if (change.pressed) change.consume()
                            }

                            continue
                        }

                        if (isZooming) {
                            if (pressedCount == 0) {
                                currentZoomableState?.let { zoomable ->
                                    gestureScope.launch { zoomable.settle() }
                                }
                                break
                            }

                            continue
                        }

                        val change = event.changes.firstOrNull { it.id == down.id }
                            ?: event.changes.firstOrNull { it.pressed }

                        if (change == null || !change.pressed) {
                            if (isEraser) {
                                if (didMove) {
                                    raster.endErase()
                                } else {
                                    raster.eraseTap(downPoint)
                                }
                            } else {
                                raster.endStroke()
                            }

                            break
                        }

                        change.consume()
                        didMove = true
                        val point = change.position.toRaster(raster, size)

                        if (isEraser) {
                            raster.eraseAt(point)
                        } else {
                            raster.continueStroke(point)
                        }
                    }
                }
            }
    ) {
        raster.revision

        drawImage(
            image = rasterImage,
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            filterQuality = FilterQuality.Medium
        )

        drawImage(
            image = overlayImage,
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            filterQuality = FilterQuality.Medium,
            blendMode = if (currentIsEraser) BlendMode.DstOut else BlendMode.SrcOver
        )
    }
}

@Composable
fun MediaDrawRasterView(
    raster: MediaDrawRaster,
    modifier: Modifier = Modifier
) {
    val rasterImage = remember(raster) { raster.bitmap.asImageBitmap() }

    Canvas(modifier = modifier.clipToBounds()) {
        raster.revision

        drawImage(
            image = rasterImage,
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            filterQuality = FilterQuality.Medium
        )
    }
}

private fun Offset.toRaster(raster: MediaDrawRaster, size: IntSize): Offset =
    Offset(x * raster.width / size.width, y * raster.height / size.height)
