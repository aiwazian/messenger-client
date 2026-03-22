/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.network.dto.ChatResponseDto

fun ChatResponseDto.toDomain(): Chat = Chat(
    id = id.toLong(),
    chatName = name,
    isPinned = isPinned,
    lastMessage = lastMessage?.let { dto ->
        Message(
            id = dto.id,
            senderId = dto.senderId.toLongOrNull() ?: 0L,
            chatId = dto.chatId.toLongOrNull() ?: 0L,
            text = dto.text,
            sendTime = dto.sendTime,
            isRead = dto.isRead ?: false
        )
    }
)
