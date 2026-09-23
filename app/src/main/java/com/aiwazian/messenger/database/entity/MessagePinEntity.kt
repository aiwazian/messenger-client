package com.aiwazian.messenger.database.entity

import androidx.room3.Entity
import androidx.room3.Index

/**
 * Закрепление сообщения в рамках одного чата.
 *
 * forEveryone = false — закрепление «для меня», forEveryone = true — «для всех».
 * Разделение по аккаунту такое же, как у сообщений: строка принадлежит
 * активному аккаунту.
 */
@Entity(
    tableName = "message_pin",
    primaryKeys = ["chatId", "messageId", "forEveryone", "ownerId"],
    indices = [Index(value = ["ownerId", "chatId", "pinnedAt"])]
)
data class MessagePinEntity(
    val chatId: Long,
    val messageId: Long,
    val forEveryone: Boolean,
    val pinnedAt: Long,
    val ownerId: Long = 0
)
