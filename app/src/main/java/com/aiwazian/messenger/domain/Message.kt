/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

data class Message(
    val id: Int,
    val senderId: Long,
    val chatId: Long,
    val text: String?,
    val sendTime: Long,
    val isRead: Boolean,
    val files: List<MessageFile>
)
