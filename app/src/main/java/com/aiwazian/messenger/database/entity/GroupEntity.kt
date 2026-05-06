/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aiwazian.messenger.enums.GroupType

@Entity("group")
data class GroupEntity(
    @PrimaryKey val id: Long,
    val ownerId: Long?,
    val name: String,
    val bio: String?,
    val username: String?,
    val groupType: GroupType,
    val members: Int,
    val isMember: Boolean
)
