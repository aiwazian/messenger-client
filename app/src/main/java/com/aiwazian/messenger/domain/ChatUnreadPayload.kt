/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatUnreadPayload(
    @SerialName("chatId") val chatId: Long,
    @SerialName("unreadCount") val unreadCount: Int = 0,
    @SerialName("firstUnreadMessageId") val firstUnreadMessageId: Long? = null,
    @SerialName("lastReadMessageId") val lastReadMessageId: Long? = null,
    @SerialName("isManuallyUnread") val isManuallyUnread: Boolean = false
)
