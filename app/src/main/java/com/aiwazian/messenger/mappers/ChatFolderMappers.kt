/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.database.entity.ChatFolderChatEntity
import com.aiwazian.messenger.database.entity.ChatFolderEntity
import com.aiwazian.messenger.domain.ChatFolder
import com.aiwazian.messenger.domain.ChatFolderChat
import com.aiwazian.messenger.enums.ChatFolderCategory
import com.aiwazian.messenger.network.dto.ChatFolderChatDto
import com.aiwazian.messenger.network.dto.ChatFolderDto

private const val CATEGORY_SEPARATOR = ","

fun ChatFolderDto.toDomain(): ChatFolder {
    return ChatFolder(
        id = id,
        name = name,
        sortOrder = sortOrder,
        categories = categories,
        chats = chats.map { it.toDomain() }
    )
}

fun ChatFolderChatDto.toDomain(): ChatFolderChat {
    return ChatFolderChat(
        chatId = chatId.toLongOrNull() ?: 0L,
        isIncluded = isIncluded,
        isPinned = isPinned,
        sortOrder = sortOrder
    )
}

fun ChatFolderEntity.toDomain(chats: List<ChatFolderChatEntity>): ChatFolder {
    return ChatFolder(
        id = id,
        name = name,
        sortOrder = sortOrder,
        categories = categories.toCategories(),
        chats = chats.map { it.toDomain() }
    )
}

fun ChatFolderChatEntity.toDomain(): ChatFolderChat {
    return ChatFolderChat(
        chatId = chatId,
        isIncluded = isIncluded,
        isPinned = isPinned,
        sortOrder = sortOrder
    )
}

fun ChatFolder.toEntity(): ChatFolderEntity {
    return ChatFolderEntity(
        id = id,
        name = name,
        sortOrder = sortOrder,
        categories = categories.joinToString(CATEGORY_SEPARATOR) { it.name }
    )
}

fun ChatFolder.toChatEntities(): List<ChatFolderChatEntity> {
    return chats.map { chat ->
        ChatFolderChatEntity(
            folderId = id,
            chatId = chat.chatId,
            isIncluded = chat.isIncluded,
            isPinned = chat.isPinned,
            sortOrder = chat.sortOrder
        )
    }
}

private fun String.toCategories(): List<ChatFolderCategory> {
    if (isBlank()) {
        return emptyList()
    }
    
    return split(CATEGORY_SEPARATOR).mapNotNull { name ->
        ChatFolderCategory.entries.firstOrNull { it.name == name }
    }
}
