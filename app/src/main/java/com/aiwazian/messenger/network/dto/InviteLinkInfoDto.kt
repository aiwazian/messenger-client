/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InviteLinkInfoDto(
    @SerialName("chatId") val chatId: Long,
    @SerialName("name") val name: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("membersCount") val membersCount: Int? = null,
    @SerialName("isBanned") val isBanned: Boolean? = null,
    @SerialName("isJoined") val isJoined: Boolean? = null
)
