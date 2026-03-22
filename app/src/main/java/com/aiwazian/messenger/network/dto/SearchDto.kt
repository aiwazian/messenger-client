/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchResponseDto(
    @SerialName("chatId") val chatId: String,
    @SerialName("name") val name: String
)

@Serializable
data class UsernameAvailableResponseDto(
    @SerialName("available") val available: Boolean
)
