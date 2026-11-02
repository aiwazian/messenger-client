/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.notification.exception.chats

import com.aiwazian.messenger.domain.Chat

data class SelectExceptionChatUiState(
    val query: String = "",
    val chats: List<Chat> = emptyList()
)
