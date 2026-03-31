/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: Int = 0,
    val senderId: Long = 0,
    val chatId: Long = 0,
    val text: String? = null,
    val sendTime: Long = 0,
    val isRead: Boolean = false,
    val files: List<MessageFile> = emptyList()
)
