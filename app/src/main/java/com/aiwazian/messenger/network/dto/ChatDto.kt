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
    @SerialName("isPinned") val isPinned: Boolean,
    @SerialName("lastMessage") val lastMessage: MessageDto? = null
)
