/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.DropdownMenuPopupPositionProvider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorPosition
import androidx.compose.material3.MenuDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupProperties

@Composable
fun AppDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    properties: PopupProperties = PopupProperties(focusable = true),
    popupPositionProvider: DropdownMenuPopupPositionProvider = MenuDefaults.rememberDropdownMenuPopupPositionProvider(
        MenuAnchorPosition.Below
    ),
    shape: Shape = MaterialTheme.shapes.medium,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable (ColumnScope.() -> Unit)
) {
    DropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        popupPositionProvider = popupPositionProvider,
        properties = properties,
    ) {
        Column(
            modifier = Modifier
                .clip(shape)
                .background(containerColor)
        ) {
            content()
        }
    }
}

/**
 * Прижимает меню к правому краю якоря и раскрывает его вниз.
 *
 * Штатный [MenuAnchorPosition.End] здесь не подходит: он ставит меню сбоку от
 * якоря, так открываются подменю. Строки настроек занимают всю ширину, справа
 * от них места нет, и такое меню отскакивает обратно к левому краю.
 *
 * @param offset сдвиг относительно вычисленной позиции.
 */
@Composable
fun rememberRightAlignedDropdownMenuPositionProvider(
    offset: DpOffset = DpOffset.Zero
): DropdownMenuPopupPositionProvider {
    val density = LocalDensity.current
    
    return remember(density, offset) {
        RightAlignedDropdownMenuPositionProvider(offset = offset, density = density)
    }
}

private class RightAlignedDropdownMenuPositionProvider(
    private val offset: DpOffset,
    private val density: Density
) : DropdownMenuPopupPositionProvider {
    
    /** Меню прижато вправо, поэтому и раскрываться должно из правого верхнего угла. */
    override val transformOrigin: TransformOrigin = TransformOrigin(
        pivotFractionX = 1f, pivotFractionY = 0f
    )
    
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val offsetX = with(density) { offset.x.roundToPx() }
        val offsetY = with(density) { offset.y.roundToPx() }
        
        val x = (anchorBounds.right - popupContentSize.width + offsetX).coerceIn(
            0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        )
        
        val belowY = anchorBounds.bottom + offsetY
        val aboveY = anchorBounds.top - popupContentSize.height - offsetY
        
        /*
         * Снизу места не хватило — открываемся вверх, но только если сверху оно
         * есть. Иначе меню лучше прижать к нижнему краю окна, чем увести за
         * верхнюю границу.
         */
        val y = if (belowY + popupContentSize.height <= windowSize.height || aboveY < 0) {
            belowY.coerceAtMost((windowSize.height - popupContentSize.height).coerceAtLeast(0))
        } else {
            aboveY
        }
        
        return IntOffset(x, y)
    }
}
