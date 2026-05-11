/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import com.aiwazian.messenger.enums.ChannelType

data class Channel(
    val id: Long,
    val ownerId: Long?,
    val name: String,
    val bio: String?,
    val subscribers: Int,
    val removedUser: Int?,
    val channelType: ChannelType,
    val username: String?,
    val isSubscribed: Boolean,
    val avatars: List<Avatar> = emptyList()
)
