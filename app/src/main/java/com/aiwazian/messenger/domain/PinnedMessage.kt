package com.aiwazian.messenger.domain

import kotlinx.serialization.Serializable

data class PinnedMessage(
    val messageId: Long,
    val chatId: Long,
    val forEveryone: Boolean,
    val pinnedAt: Long,
    val message: Message?
)

@Serializable
data class MessagePinPayload(
    val chatId: Long,
    val messageId: Long,
    val forEveryone: Boolean = false,
    val pinnedAt: Long = 0
)
