package com.aiwazian.messenger.data

import androidx.compose.ui.graphics.vector.ImageVector

data class TopBarAction(
    val icon: ImageVector,
    val onClick: (() -> Unit)? = null,
    val dropdownActions: List<DropdownMenuAction> = emptyList()
)
