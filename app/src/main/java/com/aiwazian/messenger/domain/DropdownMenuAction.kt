/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector

data class DropdownMenuAction(
    val icon: ImageVector,
    @param:StringRes val textResId: Int,
    val onClick: () -> Unit
)
