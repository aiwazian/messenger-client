/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatResponseDto(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("isPinned") val isPinned: Boolean = false,
    @SerialName("lastMessage") val lastMessage: MessageDto? = null,
    @SerialName("unreadCount") val unreadCount: Int = 0,
    @SerialName("firstUnreadMessageId") val firstUnreadMessageId: Long? = null,
    @SerialName("isManuallyUnread") val isManuallyUnread: Boolean = false
)
