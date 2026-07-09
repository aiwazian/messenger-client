/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.database.entity.MessageEntity
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.domain.MessageAttachment
import com.aiwazian.messenger.domain.MessageReadInfo
import com.aiwazian.messenger.network.dto.MessageAttachmentDto
import com.aiwazian.messenger.network.dto.MessageDto

fun MessageDto.toDomain(): Message = Message(
    id = id,
    senderId = senderId,
    chatId = chatId,
    text = text,
    sendTime = sendTime,
    editedAt = editedAt,
    isRead = isRead ?: false,
    status = com.aiwazian.messenger.enums.MessageStatus.SENT,
    messageType = messageType,
    systemMessageEventType = systemEventType,
    attachments = attachments.map { it.toDomain(messageId = id) },
    readInfo = readInfo?.map { it.toDomain() }
)

fun com.aiwazian.messenger.network.dto.MessageReadInfoDto.toDomain() = MessageReadInfo(
    userId = userId,
    firstName = firstName,
    lastName = lastName,
    readAt = readAt
)

fun MessageAttachmentDto.toDomain(messageId: Long) = MessageAttachment(
    fileId = fileId,
    messageId = messageId,
    name = name,
    size = size,
    extension = name.substringAfterLast('.', ""),
    status = status,
    progress = 0,
    localUri = null,
    type = type,
    sortOrder = sortOrder
)

fun MessageEntity.toDomain(attachments: List<MessageAttachment> = emptyList()) = Message(
    id = id,
    senderId = senderId,
    chatId = chatId,
    text = text,
    sendTime = sendTime,
    editedAt = editedAt,
    isRead = isRead,
    status = status,
    messageType = messageType,
    systemMessageEventType = systemMessageEventType,
    attachments = attachments
)

fun Message.toEntity() = MessageEntity(
    id = id,
    senderId = senderId,
    chatId = chatId,
    text = text,
    sendTime = sendTime,
    editedAt = editedAt,
    isRead = isRead,
    messageType = messageType,
    systemMessageEventType = systemMessageEventType,
    status = status
)
