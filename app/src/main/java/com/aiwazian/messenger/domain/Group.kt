/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import com.aiwazian.messenger.enums.GroupType

data class Group(
    val id: Long = 0,
    val ownerId: Long? = null,
    val name: String = "",
    val bio: String? = null,
    val username: String? = null,
    val groupType: GroupType = GroupType.PRIVATE,
    val members: Int = 0
)
