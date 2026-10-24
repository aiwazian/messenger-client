/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.aiwazian.messenger.enums.AttachmentType

@Entity(
    tableName = "chat_media",
    indices = [Index(value = ["ownerId", "chatId", "type"])]
)
data class ChatMediaEntity(
    @PrimaryKey
    val id: Int,
    val chatId: Long,
    val fileId: String,
    val messageId: Long,
    val senderId: Long,
    val name: String,
    val size: Long,
    val mimeType: String,
    val type: AttachmentType,
    val sendTime: Long,
    @ColumnInfo(defaultValue = "0")
    val ownerId: Long = 0,
    @ColumnInfo(defaultValue = "0")
    val spoiler: Boolean = false
)
