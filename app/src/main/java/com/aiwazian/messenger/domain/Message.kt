/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import com.aiwazian.messenger.enums.MessageType
import com.aiwazian.messenger.enums.SystemMessageEventType

data class Message(
    val id: Long,
    val senderId: Long,
    val chatId: Long,
    val text: String?,
    val sendTime: Long,
    val isRead: Boolean,
    val messageType: MessageType,
    val systemMessageEventType: SystemMessageEventType?,
    val attachments: List<MessageAttachment>
)
