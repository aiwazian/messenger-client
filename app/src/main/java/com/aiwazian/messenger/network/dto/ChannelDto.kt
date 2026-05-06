/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dto

import com.aiwazian.messenger.enums.ChannelType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChannelResponseDto(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("username") val username: String? = null,
    @SerialName("bio") val bio: String? = null,
    @SerialName("ownerId") val ownerId: Long? = null,
    @SerialName("channelType") val channelType: ChannelType = ChannelType.PRIVATE,
    @SerialName("subscribers") val subscribers: Int = 0,
    @SerialName("removedUser") val removedUser: Int? = null,
    @SerialName("isSubscribed") val isSubscribed: Boolean = false,
    @SerialName("isOwner") val isOwner: Boolean? = null
)

@Serializable
data class CreateChannelRequestDto(
    @SerialName("name") val name: String,
    @SerialName("bio") val bio: String? = null,
    @SerialName("channelType") val channelType: ChannelType = ChannelType.PRIVATE,
    @SerialName("username") val username: String? = null
)

@Serializable
data class UpdateChannelRequestDto(
    @SerialName("name") val name: String? = null,
    @SerialName("bio") val bio: String? = null,
    @SerialName("channelType") val channelType: ChannelType? = null,
    @SerialName("username") val username: String? = null
)
