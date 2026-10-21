/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.enums

import kotlinx.serialization.Serializable

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
