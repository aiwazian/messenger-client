/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Тело POST /chats/{chatId}/messages/read.
 *
 * upToMessageId — максимальный id, который реально показался на экране.
 * null — «прочитан весь чат» (кнопка «вниз»).
 */
@Serializable
data class MarkReadRequestDto(
    @SerialName("upToMessageId") val upToMessageId: String? = null
)

@Serializable
data class ChatReadStateDto(
    @SerialName("chatId") val chatId: Long,
    @SerialName("unreadCount") val unreadCount: Int = 0,
    @SerialName("lastReadMessageId") val lastReadMessageId: Long? = null,
    @SerialName("firstUnreadMessageId") val firstUnreadMessageId: Long? = null
)
