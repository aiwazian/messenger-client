/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InviteLinkInfoDto(
    @SerialName("chatId") val chatId: String,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null,
    @SerialName("membersCount") val membersCount: Int,
    @SerialName("isBanned") val isBanned: Boolean,
    @SerialName("isJoined") val isJoined: Boolean,
    @SerialName("type") val type: String
)
