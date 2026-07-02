/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchResponseDto(
    @SerialName("chatId") val chatId: Long,
    @SerialName("name") val name: String,
    @SerialName("username") val username: String? = null,
)

@Serializable
data class UsernameAvailableResponseDto(
    @SerialName("available") val available: Boolean
)

@Serializable
data class ResolveUsernameResponseDto(
    @SerialName("chatId") val chatId: Long,
    @SerialName("isBanned") val isBanned: Boolean = false
)
