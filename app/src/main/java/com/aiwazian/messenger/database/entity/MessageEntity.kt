package com.aiwazian.messenger.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("message")
data class MessageEntity(
    @PrimaryKey var id: Int,
    val senderId: Long,
    val chatId: Long,
    val text: String? = null,
    val sendTime: Long = 0,
    var isRead: Boolean = false
)