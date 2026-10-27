/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.DropdownMenuPopupPositionProvider
import androidx.compose.material3.MenuAnchorPosition
import androidx.compose.material3.MenuDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

private object TopLeftPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ) = IntOffset.Zero
}

@Composable
fun AppDropdownMenuPopup(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    popupPositionProvider: DropdownMenuPopupPositionProvider = MenuDefaults.rememberDropdownMenuPopupPositionProvider(
        MenuAnchorPosition.Below
    ),
    properties: PopupProperties = PopupProperties(focusable = false),
    scrimColor: Color? = null,
    content: @Composable (ColumnScope.() -> Unit)
) {
    val currentDismissRequest by rememberUpdatedState(onDismissRequest)

    BackHandler(enabled = expanded && !properties.focusable) {
        currentDismissRequest()
    }

    if (expanded && (scrimColor != null || !properties.focusable)) {
        Popup(
            popupPositionProvider = TopLeftPositionProvider,
            properties = PopupProperties(focusable = false, clippingEnabled = false)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(scrimColor ?: Color.Transparent)
                    .pointerInput(Unit) { detectTapGestures { currentDismissRequest() } }
            )
        }
    }
    
    DropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        popupPositionProvider = popupPositionProvider,
        properties = properties,
    ) {
        content()
    }
}
