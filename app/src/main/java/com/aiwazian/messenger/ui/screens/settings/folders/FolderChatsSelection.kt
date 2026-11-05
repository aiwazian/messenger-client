/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.folders

import com.aiwazian.messenger.enums.ChatFolderCategory

data class FolderChatsSelection(
    val chatIds: List<Long> = emptyList(),
    val categories: List<ChatFolderCategory> = emptyList()
)
