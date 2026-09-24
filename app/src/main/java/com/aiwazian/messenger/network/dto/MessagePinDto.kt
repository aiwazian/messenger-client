package com.aiwazian.messenger.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PinMessageRequestDto(
    @SerialName("forEveryone") val forEveryone: Boolean
)

@Serializable
data class MessagePinResponseDto(
    @SerialName("chatId") val chatId: Long,
    @SerialName("messageId") val messageId: Long,
    @SerialName("forEveryone") val forEveryone: Boolean = false,
    @SerialName("pinnedAt") val pinnedAt: Long = 0,
    @SerialName("message") val message: MessageDto? = null
)
