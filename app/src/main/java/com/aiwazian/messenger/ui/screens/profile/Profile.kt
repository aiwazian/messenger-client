/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.profile

import com.aiwazian.messenger.enums.ChannelType

sealed interface Profile {
    val id: Long
    val bio: String?
    val username: String?
    
    data class User(
        override val id: Long = 0,
        val firstName: String = "",
        val lastName: String? = null,
        override val username: String? = null,
        override val bio: String? = null,
        val dateOfBirth: Long? = null,
        val lastSeen: Long? = null,
    ) : Profile
    
    data class Channel(
        override val id: Long = 0,
        val ownerId: Long? = null,
        val name: String = "",
        override val bio: String? = null,
        override val username: String? = null,
        val subscribers: Int = 0,
        val removedUser: Int? = null,
        val channelType: ChannelType = ChannelType.PRIVATE,
        val isSubscribed: Boolean = false
    ) : Profile
    
    data class Group(
        override val id: Long = 0,
        val ownerId: Long? = null,
        val name: String = "",
        override val bio: String? = null,
        override val username: String? = null,
        val members: Int = 0
    ) : Profile
}