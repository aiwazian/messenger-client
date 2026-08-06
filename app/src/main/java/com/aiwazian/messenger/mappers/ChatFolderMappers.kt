/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.domain.ChatFolder
import com.aiwazian.messenger.domain.ChatFolderChat
import com.aiwazian.messenger.network.dto.ChatFolderChatDto
import com.aiwazian.messenger.network.dto.ChatFolderDto

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
        isPinned = isPinned,
        sortOrder = sortOrder
    )
}
