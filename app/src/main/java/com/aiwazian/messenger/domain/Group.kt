/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import com.aiwazian.messenger.enums.GroupType

data class Group(
    val id: Long,
    val ownerId: Long?,
    val name: String,
    val bio: String?,
    val username: String?,
    val groupType: GroupType,
    val members: Int,
    val isMember: Boolean,
    val avatars: List<Avatar> = emptyList()
)
