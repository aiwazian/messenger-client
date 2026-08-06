/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.folders.editor

import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.enums.ChatFolderCategory

data class ChatFolderEditorUiState(
    val folderId: Int? = null,
    val name: String = "",
    val selectedChatIds: List<Long> = emptyList(),
    val selectedCategories: List<ChatFolderCategory> = emptyList(),
    val chats: List<Chat> = emptyList(),
    val isSaving: Boolean = false
) {
    
    /** Экран один на создание и редактирование: режим задаёт переданный id папки. */
    val isEditing: Boolean
        get() = folderId != null
    
    /** Пустую папку создавать нечего: нужны либо чаты, либо категория. */
    val canSave: Boolean
        get() = name.isNotBlank() && !isSaving &&
                (selectedChatIds.isNotEmpty() || selectedCategories.isNotEmpty())
}

sealed interface ChatFolderEditorSideEffect {
    data object FolderSaved : ChatFolderEditorSideEffect
}
