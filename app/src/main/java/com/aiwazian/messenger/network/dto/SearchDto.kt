/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchResponseDto(
    @SerialName("type") val type: String,
    @SerialName("chatId") val chatId: String,
    @SerialName("name") val name: String,
    @SerialName("fileId") val fileId: String? = null,
    @SerialName("size") val size: String? = null,
    @SerialName("mimeType") val mimeType: String? = null,
    @SerialName("messageId") val messageId: String? = null,
    @SerialName("senderName") val senderName: String? = null,
    @SerialName("createdAt") val createdAt: String? = null
)

@Serializable
data class UsernameAvailableResponseDto(
    @SerialName("available") val available: Boolean
)
