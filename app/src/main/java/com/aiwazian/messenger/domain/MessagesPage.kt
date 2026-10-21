/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain
data class MessagesPage(
    val messages: List<Message>,
    val hasMoreBefore: Boolean,
    val hasMoreAfter: Boolean,
    val unreadCount: Int = 0,
    val firstUnreadMessageId: Long? = null
)

data class MessageSearchHit(
    val id: Long,
    val senderId: Long,
    val text: String?,
    val sendTime: Long
)

data class MessageSearchPage(
    val items: List<MessageSearchHit>,
    val nextCursorId: Long?,
    val scannedAll: Boolean,
    val total: Int? = null,
    val totalIsExact: Boolean = true
)
