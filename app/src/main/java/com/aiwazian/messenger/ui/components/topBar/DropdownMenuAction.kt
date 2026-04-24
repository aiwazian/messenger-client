/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components.topBar

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector

data class DropdownMenuAction(
    val icon: ImageVector,
    @param:StringRes val textResId: Int,
    val onClick: () -> Unit,
    val isDestructive: Boolean = false
)
