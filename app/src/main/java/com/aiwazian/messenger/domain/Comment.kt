/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import com.aiwazian.messenger.enums.CommentAuthorRole
import com.aiwazian.messenger.enums.MessageType

data class Comment(
    val id: Long,
    val postId: Long,
    val senderId: Long,
    val text: String?,
    val sendTime: Long,
    val messageType: MessageType = MessageType.TEXT,
    val senderRole: CommentAuthorRole = CommentAuthorRole.MEMBER,
    val senderName: String = "",
    val sticker: MessageSticker? = null,
    val attachments: List<MessageAttachment> = emptyList()
)
