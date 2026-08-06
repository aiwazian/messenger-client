/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.folders

import com.aiwazian.messenger.domain.ChatFolder

data class ChatFoldersUiState(
    val folders: List<ChatFolder> = emptyList(),
    val folderPendingDeletion: ChatFolder? = null
)
