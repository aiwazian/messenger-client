/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.folders

import com.aiwazian.messenger.enums.ChatFolderCategory

/**
 * Результат выбора содержимого папки.
 *
 * Возвращается на предыдущий экран через ResultEffect из Navigation 3:
 * поимённые чаты и категории целиком приходят вместе, одним событием.
 */
data class FolderChatsSelection(
    val chatIds: List<Long> = emptyList(),
    val categories: List<ChatFolderCategory> = emptyList()
)
