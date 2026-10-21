/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.notification.category

import com.aiwazian.messenger.domain.Chat

data class NotificationCategoryUiState(
    val isLoading: Boolean = true,
    val exceptions: List<NotificationExceptionItem> = emptyList(),
    val showDeleteAllDialog: Boolean = false
)

data class NotificationExceptionItem(
    val chatId: Long,
    val chat: Chat?,
    val enabled: Boolean
)
