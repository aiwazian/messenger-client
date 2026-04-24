/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components.topBar

import androidx.compose.ui.graphics.vector.ImageVector

data class TopBarAction(
    val icon: ImageVector,
    val onClick: (() -> Unit)? = null,
    val dropdownActions: List<DropdownMenuAction> = emptyList()
)
