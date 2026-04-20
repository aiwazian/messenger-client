/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.database.entity.MessageEntity
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.domain.MessageFile
import com.aiwazian.messenger.network.dto.MessageDto
import com.aiwazian.messenger.network.dto.MessageFileDto

fun MessageDto.toDomain(): Message = Message(
    id = id,
    senderId = senderId,
    chatId = chatId,
    text = if (text == "null") null else text,
    sendTime = sendTime,
    isRead = isRead ?: false,
    files = files.map { it.toDomain() }
)

fun MessageFileDto.toDomain() = MessageFile(
    id = id,
    name = name,
    size = size,
    extension = name.substringAfterLast('.', ""),
    status = status,
    progress = 0,
    localUri = null
)

fun MessageEntity.toDomain(files: List<MessageFile> = emptyList()) = Message(
    id = id,
    senderId = senderId,
    chatId = chatId,
    text = text,
    sendTime = sendTime,
    isRead = isRead,
    files = files
)

fun Message.toEntity() = MessageEntity(
    id = id,
    senderId = senderId,
    chatId = chatId,
    text = text,
    sendTime = sendTime,
    isRead = isRead
)
