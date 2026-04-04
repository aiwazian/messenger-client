/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.PopupProperties
import androidx.compose.material3.DropdownMenu as MaterialDropdownMenu

@Composable
fun DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable (ColumnScope.() -> Unit)
) {
    MaterialDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = MaterialTheme.shapes.large,
        properties = properties,
        content = content
    )
}
