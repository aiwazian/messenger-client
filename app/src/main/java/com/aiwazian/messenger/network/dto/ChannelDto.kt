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
    @SerialName("removedUsers") val removedUsers: Int? = null,
    @SerialName("isSubscribed") val isSubscribed: Boolean = false,
    @SerialName("isOwner") val isOwner: Boolean? = null,
    @SerialName("noCopy") val noCopy: Boolean = false,
    @SerialName("avatars") val avatars: List<AvatarDto> = emptyList()
)

@Serializable
data class CreateChannelRequestDto(
    @SerialName("name") val name: String,
    @SerialName("bio") val bio: String? = null,
    @SerialName("channelType") val channelType: ChannelType = ChannelType.PRIVATE,
    @SerialName("username") val username: String? = null,
    @SerialName("noCopy") val noCopy: Boolean? = null
)

@Serializable
data class UpdateChannelRequestDto(
    @SerialName("name") val name: String? = null,
    @SerialName("bio") val bio: String? = null,
    @SerialName("channelType") val channelType: ChannelType? = null,
    @SerialName("username") val username: String? = null,
    @SerialName("noCopy") val noCopy: Boolean? = null
)

/**
 * Тело запроса на включение или выключение запрета копирования.
 *
 * Используется и для каналов, и для групп.
 */
@Serializable
data class SetNoCopyRequestDto(
    @SerialName("noCopy") val noCopy: Boolean
)
