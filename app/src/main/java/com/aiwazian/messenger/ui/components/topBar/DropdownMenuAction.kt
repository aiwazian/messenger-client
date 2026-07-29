/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components.topBar

import androidx.compose.ui.graphics.vector.ImageVector
import com.aiwazian.messenger.utils.UiText

data class DropdownMenuAction(
    val icon: ImageVector,
    val text: UiText,
    val onClick: (() -> Unit)? = null,
    val isDestructive: Boolean = false,
    /**
     * Не действие, а пояснение к меню: выносится в отдельную группу под
     * остальными пунктами (например, «Копирование и пересылка запрещены»).
     */
    val isNotice: Boolean = false
)
