/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateInviteLinkRequestDto(
    @SerialName("expiresAt") val expiresAt: Long? = null,
    @SerialName("maxUses") val maxUses: Int? = null
)

@Serializable
data class InviteLinkResponseDto(
    @SerialName("id") val id: Long,
    @SerialName("code") val code: String,
    @SerialName("expiresAt") val expiresAt: Long? = null,
    @SerialName("maxUses") val maxUses: Int? = null,
    @SerialName("uses") val uses: Int? = null
)

@Serializable
data class InviteLinkInfoDto(
    @SerialName("chatId") val chatId: Long,
    @SerialName("name") val name: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("membersCount") val membersCount: Int? = null,
    @SerialName("isBanned") val isBanned: Boolean? = null,
    @SerialName("isJoined") val isJoined: Boolean? = null
)
