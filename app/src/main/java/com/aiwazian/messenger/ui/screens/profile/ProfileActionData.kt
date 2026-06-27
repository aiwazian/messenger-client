/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.profile

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.aiwazian.messenger.utils.UiText

data class ProfileActionData(
    val onClick: () -> Unit,
    val icon: ImageVector,
    val text: UiText,
    val contentColor: Color = Color.Unspecified
)
