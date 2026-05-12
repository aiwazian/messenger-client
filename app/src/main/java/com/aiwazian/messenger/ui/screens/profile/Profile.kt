/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.profile

sealed interface Profile {
    val bio: String?
    val username: String?
    
    data class User(
        override val bio: String? = null,
        override val username: String? = null,
        val dateOfBirth: Long? = null,
        val lastSeen: Long? = null,
    ) : Profile
    
    data class Channel(
        override val bio: String? = null,
        override val username: String? = null,
        val ownerId: Long? = null,
        val subscribers: Int = 0,
        val isSubscribed: Boolean = false,
    ) : Profile
    
    data class Group(
        override val bio: String? = null,
        override val username: String? = null,
        val ownerId: Long? = null,
        val members: Int = 0,
        val isMember: Boolean = false,
    ) : Profile
}
