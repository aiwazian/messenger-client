/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.app

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

/**
 * Пункт выпадающего меню.
 *
 * [text] — строка, а не Composable-слот: все вызовы в приложении всё равно
 * клали внутрь один Text, а слот позволял каждому меню завести свои размеры и
 * цвета. Со строкой типографика пункта живёт только здесь.
 *
 * [supportingText] — вторая строка под названием для случаев вроде «имя и время
 * просмотра»: тоже обычная строка, а не возврат к произвольной разметке.
 */
@Composable
fun AppDropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    contentPadding: PaddingValues = MenuDefaults.DropdownMenuItemContentPadding,
    interactionSource: MutableInteractionSource? = null
) {
    val colors = MenuItemColors(
        textColor = contentColor,
        leadingIconColor = contentColor,
        trailingIconColor = contentColor,
        disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
    
    DropdownMenuItem(
        text = {
            if (supportingText == null) {
                Text(
                    text = text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Column {
                    Text(
                        text = text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = supportingText,
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        onClick = onClick,
        modifier = modifier,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        enabled = enabled,
        colors = colors,
        contentPadding = contentPadding,
        interactionSource = interactionSource
    )
}
