/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import com.aiwazian.messenger.enums.ChatType

data class NotificationSettings(
    val privateChats: Boolean = true,
    val groups: Boolean = true,
    val channels: Boolean = true
) {
    
    fun isEnabledFor(chatType: ChatType): Boolean = when (chatType) {
        ChatType.PRIVATE -> privateChats
        ChatType.GROUP -> groups
        ChatType.CHANNEL -> channels
        ChatType.UNKNOWN -> true
    }
}
