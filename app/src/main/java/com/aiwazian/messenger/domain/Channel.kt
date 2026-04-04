/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import com.aiwazian.messenger.enums.ChannelType

data class Channel(
    val id: Long = 0,
    val ownerId: Long? = null,
    val name: String = "",
    val bio: String? = null,
    val subscribers: Int = 0,
    val removedUser: Int? = null,
    val channelType: ChannelType = ChannelType.PRIVATE,
    val username: String? = null,
    val inviteLink: String? = null,
    val isSubscribed: Boolean = false
)
