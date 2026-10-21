/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.notification.exception

import com.aiwazian.messenger.domain.Chat

data class NotificationExceptionUiState(
    val chat: Chat? = null,
    val notificationsEnabled: Boolean = true,
    val hasException: Boolean = false
)
