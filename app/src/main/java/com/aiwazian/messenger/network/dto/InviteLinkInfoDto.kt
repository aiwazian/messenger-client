/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InviteLinkInfoDto(
    @SerialName("channelId") val channelId: String,
    @SerialName("channelName") val channelName: String,
    @SerialName("description") val description: String? = null,
    @SerialName("subscribersCount") val subscribersCount: Int
)
