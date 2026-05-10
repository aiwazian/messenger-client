/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.main

import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.domain.User
import com.aiwazian.messenger.enums.ThemeOption

data class MainUiState(
    val me: User = User(
        id = 0,
        firstName = "",
        lastName = null,
        bio = null,
        username = null,
        dateOfBirth = null,
        lastSeen = null,
        avatars = emptyList()
    ),
    val hasPasscode: Boolean = false,
    val chats: List<Chat> = emptyList(),
    val theme: ThemeOption = ThemeOption.SYSTEM,
    val selectedChatIds: Set<Long> = emptySet()
)
