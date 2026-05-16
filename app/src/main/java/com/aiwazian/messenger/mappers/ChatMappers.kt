/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.mappers

import android.net.Uri
import com.aiwazian.messenger.database.entity.ChatEntity
import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.network.dto.ChatResponseDto
import com.aiwazian.messenger.utils.UiText

fun ChatResponseDto.toDomain(): Chat = Chat(
    id = id,
    chatName = UiText.DynamicString(name),
    isPinned = isPinned,
    avatarUri = null,
    lastMessage = lastMessage?.toDomain()
)

fun ChatResponseDto.toEntity(userId: Long): ChatEntity = ChatEntity(
    userId = userId,
    chatId = id,
    isPinned = isPinned
)

fun ChatEntity.toDomain(name: UiText, avatarUri: Uri?, lastMessage: Message? = null): Chat = Chat(
    id = chatId,
    chatName = name,
    isPinned = isPinned,
    avatarUri = avatarUri,
    lastMessage = lastMessage
)

fun Chat.toEntity(userId: Long): ChatEntity = ChatEntity(
    userId = userId,
    chatId = id,
    isPinned = isPinned
)
