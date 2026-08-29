/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Ответ GET /chats/{chatId}/messages/window.
 * messages всегда отсортированы от старых к новым.
 */
@Serializable
data class MessagesWindowDto(
    @SerialName("messages") val messages: List<MessageDto> = emptyList(),
    @SerialName("hasMoreBefore") val hasMoreBefore: Boolean = false,
    @SerialName("hasMoreAfter") val hasMoreAfter: Boolean = false,
    @SerialName("unreadCount") val unreadCount: Int = 0,
    @SerialName("firstUnreadMessageId") val firstUnreadMessageId: Long? = null
)

@Serializable
data class MessageSearchHitDto(
    @SerialName("id") val id: Long,
    @SerialName("senderId") val senderId: Long,
    @SerialName("text") val text: String? = null,
    @SerialName("sendTime") val sendTime: Long
)

@Serializable
data class MessageSearchResponseDto(
    @SerialName("items") val items: List<MessageSearchHitDto> = emptyList(),
    @SerialName("nextCursorId") val nextCursorId: Long? = null,
    @SerialName("scannedAll") val scannedAll: Boolean = true,
    /** Всего совпадений в чате. Сервер считает его только для первой страницы. */
    @SerialName("total") val total: Int? = null,
    @SerialName("totalIsExact") val totalIsExact: Boolean = true
)
