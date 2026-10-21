/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatNotificationSettingDto(
    @SerialName("chatId") val chatId: Long,
    @SerialName("enabled") val enabled: Boolean
)

@Serializable
data class UpdateChatNotificationSettingRequestDto(
    @SerialName("enabled") val enabled: Boolean
)
