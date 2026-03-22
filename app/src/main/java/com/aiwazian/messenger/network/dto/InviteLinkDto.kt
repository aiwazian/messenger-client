/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateInviteLinkRequestDto(
    @SerialName("channelId") val channelId: String? = null,
    @SerialName("groupId") val groupId: String? = null,
    @SerialName("expiresInSeconds") val expiresInSeconds: Int? = null,
    @SerialName("maxUses") val maxUses: Int? = null
)

@Serializable
data class InviteLinkResponseDto(
    @SerialName("id") val id: String,
    @SerialName("chatId") val chatId: String,
    @SerialName("code") val code: String,
    @SerialName("link") val link: String,
    @SerialName("expiresAt") val expiresAt: String? = null,
    @SerialName("isPermanent") val isPermanent: Boolean,
    @SerialName("maxUses") val maxUses: Int? = null,
    @SerialName("uses") val uses: Int
)
