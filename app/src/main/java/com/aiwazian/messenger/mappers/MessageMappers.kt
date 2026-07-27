/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.database.entity.MessageEntity
import com.aiwazian.messenger.domain.ForwardedFrom
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.domain.MessageAttachment
import com.aiwazian.messenger.domain.MessageReadInfo
import com.aiwazian.messenger.domain.MessageReplyPreview
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.ForwardSourceAccess
import com.aiwazian.messenger.network.dto.MessageAttachmentDto
import com.aiwazian.messenger.network.dto.MessageDto
import com.aiwazian.messenger.network.dto.MessageReplyPreviewDto

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
    readInfo = readInfo?.map { it.toDomain() },
    replyTo = replyTo?.toDomain() ?: replyToId?.let {
        MessageReplyPreview(messageId = it, chatId = replyToChatId)
    },
    forwardedFrom = forwardedFromChatId?.let { sourceChatId ->
        ForwardedFrom(
            chatId = sourceChatId,
            name = forwardedFromName.orEmpty(),
            access = forwardedFromAccess ?: ForwardSourceAccess.UNAVAILABLE
        )
    }
)

fun MessageReplyPreviewDto.toDomain() = MessageReplyPreview(
    messageId = id,
    chatId = chatId,
    senderId = senderId,
    senderName = senderName,
    chatName = chatName,
    text = text,
    attachmentTypes = attachmentTypes
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
    attachments = attachments,
    replyTo = replyToId?.let { originalId ->
        MessageReplyPreview(
            messageId = originalId,
            chatId = replyToChatId,
            senderId = replyToSenderId,
            senderName = replyToSenderName,
            chatName = replyToChatName,
            text = replyToText,
            attachmentTypes = replyToAttachmentTypes.toAttachmentTypes()
        )
    },
    forwardedFrom = forwardedFromChatId?.let { sourceChatId ->
        ForwardedFrom(
            chatId = sourceChatId,
            name = forwardedFromName.orEmpty(),
            access = forwardedFromAccess.toForwardSourceAccess()
        )
    }
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
    status = status,
    replyToId = replyTo?.messageId,
    replyToChatId = replyTo?.chatId,
    replyToSenderId = replyTo?.senderId,
    replyToSenderName = replyTo?.senderName,
    replyToChatName = replyTo?.chatName,
    replyToText = replyTo?.text,
    replyToAttachmentTypes = replyTo?.attachmentTypes
        ?.takeIf { it.isNotEmpty() }
        ?.joinToString(",") { it.name },
    forwardedFromChatId = forwardedFrom?.chatId,
    forwardedFromName = forwardedFrom?.name,
    forwardedFromAccess = forwardedFrom?.access?.name
)

private fun String?.toAttachmentTypes(): List<AttachmentType> = this
    ?.split(",")
    ?.mapNotNull { raw -> runCatching { AttachmentType.valueOf(raw.trim()) }.getOrNull() }
    .orEmpty()

private fun String?.toForwardSourceAccess(): ForwardSourceAccess =
    this?.let { raw -> runCatching { ForwardSourceAccess.valueOf(raw) }.getOrNull() }
        ?: ForwardSourceAccess.UNAVAILABLE
