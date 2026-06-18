/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aiwazian.messenger.enums.MessageType
import com.aiwazian.messenger.enums.SystemMessageEventType

@Entity("message")
data class MessageEntity(
    @PrimaryKey val id: Long,
    val senderId: Long,
    val chatId: Long,
    val text: String?,
    val sendTime: Long,
    val editedAt: Long? = null,
    val messageType: MessageType,
    val systemMessageEventType: SystemMessageEventType?,
    val isRead: Boolean
)
