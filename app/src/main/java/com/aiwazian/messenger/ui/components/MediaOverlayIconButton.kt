/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Кнопка поверх кадра: полупрозрачная подложка и белая иконка.
 *
 * Подложка нужна потому, что кнопка лежит на самом медиа: на светлом кадре
 * белая иконка без неё теряется. Такими кнопками подписано и качество видео,
 * и поворот кадра — иначе стоящие рядом кнопки выглядели бы чужими друг другу.
 */
@Composable
fun MediaOverlayIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    IconButton(
        onClick = onClick, modifier = modifier,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.2f)
        )
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = Color.White)
    }
}
