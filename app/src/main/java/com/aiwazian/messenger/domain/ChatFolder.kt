/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import com.aiwazian.messenger.enums.ChatFolderCategory

data class ChatFolder(
    val id: Int,
    val name: String,
    val sortOrder: Int = 0,
    val categories: List<ChatFolderCategory> = emptyList(),
    val chats: List<ChatFolderChat> = emptyList()
) {
    
    val includedChatIds: List<Long>
        get() = chats.filter { it.isIncluded }.map { it.chatId }
    
    fun contains(chatId: Long): Boolean {
        return categories.any { it.matches(chatId) } ||
                chats.any { it.chatId == chatId && it.isIncluded }
    }
    
    fun isPinned(chatId: Long): Boolean {
        return chats.firstOrNull { it.chatId == chatId }?.isPinned == true
    }
}

data class ChatFolderChat(
    val chatId: Long,
    val isIncluded: Boolean = true,
    val isPinned: Boolean = false,
    val sortOrder: Int = 0
)
