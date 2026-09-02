/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

private val MediaGridFallbackWidth = 264.dp

private val MediaGridMinHeight = 80.dp
private val MediaGridMaxHeight = 400.dp

private const val MaxLayoutDimension = 16_777_215

private const val SquareRatioTolerance = 0.05f

private enum class MediaShape { LANDSCAPE, PORTRAIT, SQUARE }

/**
 * Сетка вложений сообщения.
 *
 * @param maxWidth предел ширины всей сетки. Внутри Column(IntrinsicSize.Max) сетка
 * меряется без верхней границы и без этого предела садится на запасные 264.dp
 * вместо всей доступной ширины пузыря.
 * @param itemSizes размеры кадров в пикселях в том же порядке, что и content. По ним
 * подбирается высота одиночного кадра и раскладка пары. Неизвестный размер — IntSize.Zero.
 */
@Composable
fun ImageGridCustomLayout(
    spacing: Dp = 2.dp,
    maxWidth: Dp = MediaGridFallbackWidth,
    itemSizes: List<IntSize> = emptyList(),
    content: @Composable () -> Unit
) {
    val gap = with(LocalDensity.current) { spacing.toPx() }
    val density = LocalDensity.current
    val currentLarge = MaterialTheme.shapes.large
    
    val newCornerDp = with(density) {
        currentLarge.topStart.toPx(Size.Unspecified, density).toDp() - 2.dp
    }
    
    Layout(
        content = content, modifier = Modifier
            .widthIn(max = maxWidth)
            .heightIn(max = MediaGridMaxHeight)
            .padding(spacing)
            .clip(currentLarge.copy(all = CornerSize(newCornerDp)))
    ) { measurables, constraints ->
        val count = measurables.size.coerceAtMost(10)
        if (count == 0) return@Layout layout(0, 0) {}
        
        val fallbackWidth = (maxWidth - spacing * 2).roundToPx().coerceAtLeast(0)
        
        val width = (if (constraints.hasBoundedWidth) constraints.maxWidth
        else fallbackWidth).coerceIn(0, MaxLayoutDimension)
        
        val maxHeight = if (constraints.hasBoundedHeight) constraints.maxHeight
        else MediaGridMaxHeight.roundToPx()
        val minHeight = MediaGridMinHeight.roundToPx().coerceAtMost(maxHeight)
        
        val firstRatio = itemSizes.getOrNull(0)?.aspectRatioOrNull()
        val secondRatio = itemSizes.getOrNull(1)?.aspectRatioOrNull()
        
        val stackPair = count == 2 && shouldStackPairVertically(
            itemSizes.getOrNull(0) ?: IntSize.Zero, itemSizes.getOrNull(1) ?: IntSize.Zero
        )
        
        val rawHeight = when {
            count == 1 && firstRatio != null -> {
                (width / firstRatio).roundToInt().coerceIn(minHeight, maxHeight)
            }
            
            count == 2 && firstRatio != null && secondRatio != null -> {
                val cellRatio = (firstRatio + secondRatio) / 2f
                val total = if (stackPair) (width / cellRatio) * 2f + gap
                else ((width - gap) / 2f) / cellRatio
                
                total.roundToInt().coerceIn(minHeight, maxHeight)
            }
            
            else -> {
                if (constraints.hasBoundedHeight) constraints.maxHeight
                else (width * 0.75f).toInt()
            }
        }
        
        val height = rawHeight.coerceIn(0, MaxLayoutDimension)
        
        layout(width, height) {
            when (count) {
                1 -> {
                    val p = measurables[0].measure(Constraints.fixed(width, height))
                    p.place(0, 0)
                }
                
                2 -> {
                    if (stackPair) {
                        val itemH = (height - gap) / 2
                        val itemConstraints = Constraints.fixed(width, itemH.toInt())
                        
                        measurables.take(2).forEachIndexed { i, m ->
                            val p = m.measure(itemConstraints)
                            p.place(0, (i * (itemH + gap)).toInt())
                        }
                    } else {
                        val itemW = (width - gap) / 2
                        val itemConstraints = Constraints.fixed(itemW.toInt(), height)
                        
                        measurables.take(2).forEachIndexed { i, m ->
                            val p = m.measure(itemConstraints)
                            p.place((i * (itemW + gap)).toInt(), 0)
                        }
                    }
                }
                
                3 -> {
                    val mainW = (width * 0.6f).toInt()
                    val sideW = (width - mainW - gap).toInt()
                    val sideH = (height - gap) / 2
                    
                    val p1 = measurables[0].measure(Constraints.fixed(mainW, height))
                    val p2 = measurables[1].measure(Constraints.fixed(sideW, sideH.toInt()))
                    val p3 = measurables[2].measure(Constraints.fixed(sideW, sideH.toInt()))
                    
                    p1.place(0, 0)
                    p2.place(mainW + gap.toInt(), 0)
                    p3.place(mainW + gap.toInt(), (sideH + gap).toInt())
                }
                
                4 -> {
                    val itemW = (width - gap) / 2
                    val itemH = (height - gap) / 2
                    val itemConstraints = Constraints.fixed(itemW.toInt(), itemH.toInt())
                    
                    measurables.take(4).forEachIndexed { i, m ->
                        val p = m.measure(itemConstraints)
                        val x = (i % 2) * (itemW + gap)
                        val y = (i / 2) * (itemH + gap)
                        p.place(x.toInt(), y.toInt())
                    }
                }
                
                5 -> {
                    val rows = listOf(2, 3)
                    placeGrid(measurables, rows, width, height, gap)
                }
                
                6 -> {
                    val rows = listOf(3, 3)
                    placeGrid(measurables, rows, width, height, gap)
                }
                
                7 -> {
                    val rows = listOf(4, 3)
                    placeGrid(measurables, rows, width, height, gap)
                }
                
                8 -> {
                    val rows = listOf(2, 3, 3)
                    placeGrid(measurables, rows, width, height, gap)
                }
                
                9 -> {
                    val rows = listOf(3, 3, 3)
                    placeGrid(measurables, rows, width, height, gap)
                }
                
                10 -> {
                    val rows = listOf(3, 4, 3)
                    placeGrid(measurables, rows, width, height, gap)
                }
                
                else -> {
                    val columns = 3
                    val rowsCount = (count + columns - 1) / columns
                    val itemW = (width - gap * (columns - 1)) / columns
                    val itemH = (height - gap * (rowsCount - 1)) / rowsCount
                    val itemConstraints = Constraints.fixed(itemW.toInt(), itemH.toInt())
                    
                    measurables.take(count).forEachIndexed { i, m ->
                        val p = m.measure(itemConstraints)
                        val x = (i % columns) * (itemW + gap)
                        val y = (i / columns) * (itemH + gap)
                        p.place(x.toInt(), y.toInt())
                    }
                }
            }
        }
    }
}

private fun IntSize.aspectRatioOrNull(): Float? =
    if (width > 0 && height > 0) width.toFloat() / height.toFloat() else null

private fun IntSize.mediaShape(): MediaShape? {
    val ratio = aspectRatioOrNull() ?: return null
    
    return when {
        abs(ratio - 1f) <= SquareRatioTolerance -> MediaShape.SQUARE
        ratio > 1f -> MediaShape.LANDSCAPE
        else -> MediaShape.PORTRAIT
    }
}

/**
 * Ставить ли пару вложений друг под другом (true) или рядом (false).
 *
 * Горизонтальный кадр шире, чем выше, поэтому пара таких ложится сверху и снизу,
 * а пара вертикальных — слева и справа: так ячейка повторяет форму самого кадра
 * и его почти не приходится обрезать. Квадраты идут вниз: два квадрата в ряд дают
 * полоску в половину ширины пузыря. Если формы разные, спор решает более длинный
 * кадр: сравниваются длинные стороны обоих в пикселях, и раскладку задаёт ориентация
 * победителя. Размер хотя бы одного кадра неизвестен — остаётся раскладка в ряд.
 */
private fun shouldStackPairVertically(first: IntSize, second: IntSize): Boolean {
    val firstShape = first.mediaShape() ?: return false
    val secondShape = second.mediaShape() ?: return false
    
    if (firstShape == secondShape) return firstShape != MediaShape.PORTRAIT
    
    val longestShape =
        if (max(first.width, first.height) >= max(second.width, second.height)) firstShape
        else secondShape
    
    return longestShape != MediaShape.PORTRAIT
}

private fun Placeable.PlacementScope.placeGrid(
    measurables: List<Measurable>,
    rows: List<Int>,
    totalWidth: Int,
    totalHeight: Int,
    gap: Float
) {
    var currentIndex = 0
    val rowCount = rows.size
    val rowH = (totalHeight - gap * (rowCount - 1)) / rowCount
    
    rows.forEachIndexed { rowIndex, itemCount ->
        val y = (rowIndex * (rowH + gap)).toInt()
        val rowW = (totalWidth - gap * (itemCount - 1)) / itemCount
        
        for (i in 0 until itemCount) {
            if (currentIndex < measurables.size) {
                val p =
                    measurables[currentIndex].measure(Constraints.fixed(rowW.toInt(), rowH.toInt()))
                val x = (i * (rowW + gap)).toInt()
                p.place(x, y)
                currentIndex++
            }
        }
    }
}
