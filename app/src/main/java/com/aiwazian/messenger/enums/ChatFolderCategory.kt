/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.enums

import kotlinx.serialization.Serializable

/**
 * Категория чатов, добавленная в папку целиком.
 *
 * Хранится вместо перечисления конкретных чатов: новый чат подходящего типа
 * попадает в папку сам, без пересохранения папки.
 */
@Serializable
enum class ChatFolderCategory {
    PRIVATE_CHATS,
    CHANNELS,
    GROUPS;
    
    fun matches(chatId: Long): Boolean {
        val chatType = ChatType.fromId(chatId)
        return when (this) {
            PRIVATE_CHATS -> chatType == ChatType.PRIVATE
            CHANNELS -> chatType == ChatType.CHANNEL
            GROUPS -> chatType == ChatType.GROUP
        }
    }
}
