/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.notification.exception.chats

import com.aiwazian.messenger.domain.Chat

/**
 * Выбор чата для нового исключения.
 *
 * chats — уже отфильтрованный поиском список, экран его только рисует.
 */
data class SelectExceptionChatUiState(
    val query: String = "",
    val chats: List<Chat> = emptyList()
)
