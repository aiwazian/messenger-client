/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
    val (notices, menuActions) = actions.partition { it.isNotice }
    val (nonClickable, clickable) = menuActions.partition { it.onClick == null }
    
    val groupShapes = MenuGroupShapes(
        MaterialTheme.shapes.medium,
        MaterialTheme.shapes.medium
    )
    
    AppDropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier.widthIn(max = 200.dp)
    ) {
        if (nonClickable.isNotEmpty() || clickable.isNotEmpty()) {
            DropdownMenuGroup(
                shapes = groupShapes,
                contentPadding = PaddingValues(0.dp)
            ) {
                nonClickable.forEach { action ->
                    Row(
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 4.dp),
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
        
        if (notices.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            
            DropdownMenuGroup(
                shapes = groupShapes,
                contentPadding = PaddingValues(0.dp)
            ) {
                notices.forEach { notice ->
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = notice.text.asString(),
                            fontSize = 12.sp,
                            lineHeight = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
