package com.aiwazian.messenger.data

import kotlinx.serialization.Serializable

@Serializable
data class ReadMessagePayload(
    val chatId: Long,
    val messageId: Int
)