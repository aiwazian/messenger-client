/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity

@Entity(
    tableName = "chats",
    primaryKeys = ["userId", "chatId"]
)
data class ChatEntity(
    val userId: Long,
    val chatId: Long,
    val isPinned: Boolean,
    @ColumnInfo(defaultValue = "0") val unreadCount: Int = 0,
    val firstUnreadMessageId: Long? = null,
    @ColumnInfo(defaultValue = "0") val isManuallyUnread: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isMuted: Boolean = false
)
