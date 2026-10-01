package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.domain.Comment
import com.aiwazian.messenger.domain.MessageReplyPreview
import com.aiwazian.messenger.network.dto.CommentDto
import com.aiwazian.messenger.network.dto.CommentReplyPreviewDto

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
    isEdited = isEdited ?: false,
    editedAt = editedAt,
    replyTo = replyTo?.toDomain(),
    sticker = sticker?.toDomain(),
    attachments = attachments.map { it.toDomain(messageId = id) }
)

fun CommentReplyPreviewDto.toDomain() = MessageReplyPreview(
    messageId = id,
    senderId = senderId,
    senderName = senderName,
    text = text,
    messageType = messageType,
    stickerEmoji = stickerEmoji
)
