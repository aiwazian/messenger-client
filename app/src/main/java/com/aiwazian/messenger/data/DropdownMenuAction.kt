package com.aiwazian.messenger.data

import androidx.compose.ui.graphics.vector.ImageVector

data class DropdownMenuAction(
    val icon: ImageVector,
    val text: String,
    val onClick: () -> Unit
)