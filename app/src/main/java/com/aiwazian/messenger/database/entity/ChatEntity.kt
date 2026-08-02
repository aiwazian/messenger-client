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
    /** Сколько сообщений не прочитано: бейдж справа в ChatCard. */
    @ColumnInfo(defaultValue = "0") val unreadCount: Int = 0,
    /** С какого сообщения открывать чат. null — всё прочитано, открываем конец. */
    val firstUnreadMessageId: Long? = null,
    /**
     * Пользователь сам пометил чат непрочитанным.
     *
     * Непрочитанных сообщений при этом может не быть вовсе — бейдж рисуется пустым.
     */
    @ColumnInfo(defaultValue = "0") val isManuallyUnread: Boolean = false
)
