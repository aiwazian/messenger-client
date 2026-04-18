/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.database.entity.MessageEntity
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.domain.MessageFile
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.network.dto.MessageDto
import com.aiwazian.messenger.network.dto.MessageFileDto
import com.aiwazian.messenger.network.dto.MessageResponseDto

fun MessageResponseDto.toDomain(): Message = Message(
    id = id,
    senderId = senderId.toLong(),
    chatId = chatId.toLong(),
    text = text,
    sendTime = sendTime,
    isRead = isRead ?: false,
    files = files.map { it.toDomain() }
)

fun MessageDto.toDomain(): Message = Message(
    id = id,
    senderId = senderId.toLong(),
    chatId = chatId.toLong(),
    text = text,
    sendTime = sendTime,
    isRead = isRead ?: false,
    files = files.map { it.toDomain() }
)

fun MessageFileDto.toDomain() = MessageFile(
    id = id,
    name = name,
    size = try {
        size.toLong()
    } catch (_: Exception) {
        0L
    },
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
