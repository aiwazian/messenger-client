/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.database.entity.MessageEntity
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.domain.MessageFile
import com.aiwazian.messenger.enums.MessageType
import com.aiwazian.messenger.enums.SystemMessageEventType
import com.aiwazian.messenger.network.dto.MessageAttachmentDto
import com.aiwazian.messenger.network.dto.MessageDto

fun MessageDto.toDomain(): Message = Message(
    id = id,
    senderId = senderId,
    chatId = chatId,
    text = if (text == "null") null else text,
    sendTime = sendTime,
    isRead = isRead ?: false,
    messageType = messageType,
    systemMessageEventType = systemEventType,
    attachments = attachments.map { it.toDomain() }
)

fun MessageAttachmentDto.toDomain() = MessageFile(
    id = id,
    name = name,
    size = size,
    extension = name.substringAfterLast('.', ""),
    status = status,
    progress = 0,
    localUri = null
)

fun MessageEntity.toDomain(attachments: List<MessageFile> = emptyList()) = Message(
    id = id,
    senderId = senderId,
    chatId = chatId,
    text = text,
    sendTime = sendTime,
    isRead = isRead,
    messageType = MessageType.fromOrdinal(messageType),
    systemMessageEventType = if (systemMessageEventType != null) SystemMessageEventType.fromOrdinal(
        systemMessageEventType
    ) else null,
    attachments = attachments
)

fun Message.toEntity() = MessageEntity(
    id = id,
    senderId = senderId,
    chatId = chatId,
    text = text,
    sendTime = sendTime,
    isRead = isRead,
    messageType = messageType.ordinal,
    systemMessageEventType = systemMessageEventType?.ordinal
)
