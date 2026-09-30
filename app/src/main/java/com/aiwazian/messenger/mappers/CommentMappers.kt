package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.domain.Comment
import com.aiwazian.messenger.network.dto.CommentDto

fun CommentDto.toDomain(): Comment = Comment(
    id = id,
    postId = postId,
    senderId = senderId,
    text = text,
    sendTime = sendTime,
    messageType = messageType,
    senderRole = senderRole,
    senderName = listOf(
        sender?.firstName.orEmpty(),
        sender?.lastName.orEmpty()
    ).filter { it.isNotBlank() }.joinToString(" "),
    sticker = sticker?.toDomain(),
    attachments = attachments.map { it.toDomain(messageId = id) }
)
