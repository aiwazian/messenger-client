/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.database.entity.ChatEntity
import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.network.dto.ChatResponseDto

fun ChatResponseDto.toDomain(): Chat = Chat(
    id = id.toLong(),
    chatName = name,
    isPinned = isPinned,
    lastMessage = lastMessage?.toDomain()
)

fun ChatResponseDto.toEntity(): ChatEntity = ChatEntity(
    chatId = id.toLong(),
    isPinned = isPinned,
    lastMessageId = lastMessage?.id
)

fun ChatEntity.toDomain(name: String, lastMessage: Message? = null): Chat = Chat(
    id = chatId,
    chatName = name,
    isPinned = isPinned,
    lastMessage = lastMessage
)

fun Chat.toEntity(): ChatEntity = ChatEntity(
    chatId = id,
    isPinned = isPinned,
    lastMessageId = lastMessage?.id
)
