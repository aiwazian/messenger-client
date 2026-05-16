/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room.Entity

@Entity(
    tableName = "chats",
    primaryKeys = ["userId", "chatId"]
)
data class ChatEntity(
    val userId: Long,
    val chatId: Long,
    val isPinned: Boolean
)
