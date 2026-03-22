/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SessionResponseDto(
    @SerialName("id") val id: Int,
    @SerialName("userId") val userId: Int,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("deviceModel") val deviceModel: String,
    @SerialName("osVersion") val osVersion: String,
    @SerialName("osName") val osName: String
)
