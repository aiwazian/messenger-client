/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MenuGroupShapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

@Composable
fun AppDropdownMenuGroup(
    modifier: Modifier = Modifier,
    shapes: MenuGroupShapes = MenuGroupShapes(
        MaterialTheme.shapes.medium,
        MaterialTheme.shapes.medium
    ),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentPadding: PaddingValues = PaddingValues.Zero,
    tonalElevation: Dp = MenuDefaults.TonalElevation,
    shadowElevation: Dp = MenuDefaults.ShadowElevation,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    DropdownMenuGroup(
        content = content,
        modifier = modifier,
        shapes = shapes,
        containerColor = containerColor,
        contentPadding = contentPadding,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        border = border
    )
}
