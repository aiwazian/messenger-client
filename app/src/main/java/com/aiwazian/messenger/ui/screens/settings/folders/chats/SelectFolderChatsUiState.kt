/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.folders.chats

import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.enums.ChatFolderCategory

data class SelectFolderChatsUiState(
    val query: String = "",
    val chats: List<Chat> = emptyList(),
    val selectedChatIds: Set<Long> = emptySet(),
    val selectedCategories: Set<ChatFolderCategory> = emptySet()
) {
    
    val hasSelection: Boolean
        get() = selectedChatIds.isNotEmpty() || selectedCategories.isNotEmpty()
    
    /** Заготовки прячутся во время поиска: искать по ним нечего. */
    val visibleCategories: List<ChatFolderCategory>
        get() = if (query.isBlank()) ChatFolderCategory.entries.toList() else emptyList()
}
