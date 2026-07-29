/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuGroupShapes
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiwazian.messenger.ui.app.AppDropdownMenuPopup
import com.aiwazian.messenger.ui.components.topBar.DropdownMenuAction

@Composable
fun MessageDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    actions: List<DropdownMenuAction>
) {
    val (nonClickable, clickable) = actions.partition { it.onClick == null }
    
    AppDropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        DropdownMenuGroup(
            shapes = MenuGroupShapes(
                MaterialTheme.shapes.medium,
                MaterialTheme.shapes.medium
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            nonClickable.forEach { action ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = action.text.asString(),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (nonClickable.isNotEmpty() && clickable.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 10.dp))
            }
            
            clickable.forEach { action ->
                val color =
                    if (action.isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                DropdownMenuItem(
                    leadingIcon = {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = null
                        )
                    },
                    text = { Text(action.text.asString()) },
                    onClick = {
                        onDismissRequest()
                        action.onClick?.invoke()
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
}
