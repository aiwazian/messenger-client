/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val MediaGridFallbackWidth = 264.dp

private val MediaGridMinHeight = 80.dp
private val MediaGridMaxHeight = 400.dp

private const val MaxLayoutDimension = 16_777_215

@Composable
fun ImageGridCustomLayout(
    spacing: Dp = 2.dp,
    singleItemAspectRatio: Float? = null,
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
            .heightIn(max = MediaGridMaxHeight)
            .padding(spacing)
            .clip(currentLarge.copy(all = CornerSize(newCornerDp)))
    ) { measurables, constraints ->
        val count = measurables.size.coerceAtMost(10)
        if (count == 0) return@Layout layout(0, 0) {}
        
        val width = (if (constraints.hasBoundedWidth) constraints.maxWidth
        else MediaGridFallbackWidth.roundToPx()).coerceIn(0, MaxLayoutDimension)
        
        val rawHeight =
            if (count == 1 && singleItemAspectRatio != null && singleItemAspectRatio > 0f) {
                val maxHeight = if (constraints.hasBoundedHeight) constraints.maxHeight
                else MediaGridMaxHeight.roundToPx()
                val minHeight = MediaGridMinHeight.roundToPx().coerceAtMost(maxHeight)
                
                (width / singleItemAspectRatio).roundToInt().coerceIn(minHeight, maxHeight)
            } else {
                if (constraints.hasBoundedHeight) constraints.maxHeight
                else (width * 0.75f).toInt()
            }
        
        val height = rawHeight.coerceIn(0, MaxLayoutDimension)
        
        layout(width, height) {
            when (count) {
                1 -> {
                    val p = measurables[0].measure(Constraints.fixed(width, height))
                    p.place(0, 0)
                }
                
                2 -> {
                    val itemW = (width - gap) / 2
                    val itemConstraints = Constraints.fixed(itemW.toInt(), height)
                    
                    measurables.take(2).forEachIndexed { i, m ->
                        val p = m.measure(itemConstraints)
                        p.place((i * (itemW + gap)).toInt(), 0)
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
