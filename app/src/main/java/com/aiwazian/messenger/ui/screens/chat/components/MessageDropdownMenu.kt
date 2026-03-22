/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.aiwazian.messenger.domain.DropdownMenuAction

@Composable
fun MessageDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    actions: List<DropdownMenuAction>
) {
    DropdownMenu(
        shape = MaterialTheme.shapes.large,
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        actions.forEach { action ->
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
                }
            )
        }
    }
}