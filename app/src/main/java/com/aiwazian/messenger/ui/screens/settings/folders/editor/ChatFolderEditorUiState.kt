/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.folders.editor

import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.enums.ChatFolderCategory

const val MAX_FOLDER_NAME_LENGTH = 15

data class ChatFolderEditorUiState(
    val folderId: Int? = null,
    val name: String = "",
    val selectedChatIds: List<Long> = emptyList(),
    val selectedCategories: List<ChatFolderCategory> = emptyList(),
    val chats: List<Chat> = emptyList(),
    val isSaving: Boolean = false
) {
    
    val isEditing: Boolean
        get() = folderId != null
    
    val canSave: Boolean
        get() = name.isNotBlank() && name.length <= MAX_FOLDER_NAME_LENGTH && !isSaving &&
                (selectedChatIds.isNotEmpty() || selectedCategories.isNotEmpty())
}

sealed interface ChatFolderEditorSideEffect {
    data object FolderSaved : ChatFolderEditorSideEffect
}
