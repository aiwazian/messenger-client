/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import android.net.Uri

data class User(
    val id: Long,
    val firstName: String,
    val lastName: String?,
    val username: String?,
    val bio: String?,
    val dateOfBirth: Long?,
    val lastSeen: Long?,
    val avatars: List<Avatar>,
    val isOnline: Boolean = false,
    val profileChannelId: Long? = null,
    val isBlocked: Boolean = false,
    val isBlockedByThem: Boolean = false
)

data class Avatar(
    val uri: Uri?,
    val fileId: String,
    val sortOrder: Int
)
