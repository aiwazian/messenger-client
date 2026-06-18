/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import com.aiwazian.messenger.enums.MessageType
import com.aiwazian.messenger.enums.SystemMessageEventType

data class MessageReadInfo(
    val userId: Long,
    val firstName: String,
    val lastName: String?,
    val readAt: Long
)

data class Message(
    val id: Long,
    val senderId: Long,
    val chatId: Long,
    val text: String?,
    val sendTime: Long,
    val editedAt: Long? = null,
    val isRead: Boolean,
    val messageType: MessageType,
    val systemMessageEventType: SystemMessageEventType?,
    val attachments: List<MessageAttachment>,
    val readInfo: List<MessageReadInfo>? = null
)
