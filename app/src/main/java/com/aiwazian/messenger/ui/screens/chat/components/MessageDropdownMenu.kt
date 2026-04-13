/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.aiwazian.messenger.ui.components.CustomDropdownMenu
import com.aiwazian.messenger.ui.components.topBar.DropdownMenuAction

@Composable
fun MessageDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    actions: List<DropdownMenuAction>
) {
    CustomDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        actions.forEach { action ->
            val color =
                if (action.isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            DropdownMenuItem(
                leadingIcon = {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                text = { Text(stringResource(action.textResId)) },
                onClick = {
                    onDismissRequest()
                    action.onClick.invoke()
                },
                colors = MenuItemColors(
                    textColor = color,
                    leadingIconColor = color,
                    trailingIconColor = color,
                    disabledTextColor = Color.Unspecified,
                    disabledLeadingIconColor = Color.Unspecified,
                    disabledTrailingIconColor = Color.Unspecified
                )
            )
        }
    }
}
