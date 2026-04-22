/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("message")
data class MessageEntity(
    @PrimaryKey val id: Int,
    val senderId: Long,
    val chatId: Long,
    val text: String?,
    val sendTime: Long,
    val messageType: Int,
    val systemMessageEventType: Int?,
    val isRead: Boolean
)
