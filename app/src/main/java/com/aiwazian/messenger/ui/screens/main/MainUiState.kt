/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.main

import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.domain.ChatFolder
import com.aiwazian.messenger.domain.User
import com.aiwazian.messenger.enums.ThemeOption
import com.aiwazian.messenger.utils.UiText

/** Папка «Все чаты» существует только на клиенте, поэтому у неё нет серверного id. */
const val ALL_CHATS_FOLDER_ID = 0

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
    val folders: List<ChatFolder> = emptyList(),
    /** Вкладки главного экрана. Первая — «Все чаты», дальше папки в порядке sortOrder. */
    val folderPages: List<ChatFolderPage> = emptyList(),
    val theme: ThemeOption = ThemeOption.SYSTEM,
    val selectedChatIds: Set<Long> = emptySet(),
    val onlineUserIds: Set<Long> = emptySet(),
    val showNotificationBottomSheet: Boolean = false,
    val askedPermission: Boolean = false,
    val showAccountDialog: Boolean = false,
    val isLocked: Boolean = false
)

data class ChatFolderPage(
    val id: Int,
    val name: UiText,
    val chats: List<Chat>
)
