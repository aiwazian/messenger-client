/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.folders.create

import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.enums.ChatFolderCategory

data class CreateChatFolderUiState(
    val name: String = "",
    val selectedChatIds: List<Long> = emptyList(),
    val selectedCategories: List<ChatFolderCategory> = emptyList(),
    val chats: List<Chat> = emptyList(),
    val isSaving: Boolean = false
) {
    
    /** Пустую папку создавать нечего: нужны либо чаты, либо категория. */
    val canSave: Boolean
        get() = name.isNotBlank() && !isSaving &&
                (selectedChatIds.isNotEmpty() || selectedCategories.isNotEmpty())
}

sealed interface CreateChatFolderSideEffect {
    data object FolderCreated : CreateChatFolderSideEffect
}
