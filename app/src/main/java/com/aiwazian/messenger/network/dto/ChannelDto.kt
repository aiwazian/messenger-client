/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.aiwazian.messenger.enums.ChannelType

@Serializable
data class ChannelResponseDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("username") val username: String? = null,
    @SerialName("bio") val bio: String? = null,
    @SerialName("ownerId") val ownerId: String? = null,
    @SerialName("channelType") val channelType: ChannelType,
    @SerialName("subscribers") val subscribers: String? = null,
    @SerialName("removedUser") val removedUser: String? = null,
    @SerialName("isSubscribed") val isSubscribed: Boolean,
    @SerialName("isOwner") val isOwner: Boolean? = null,
    @SerialName("inviteLink") val inviteLink: String? = null
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
