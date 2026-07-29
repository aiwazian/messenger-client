/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey
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
    val removedUsers: Int?,
    val isMember: Boolean,
    /** Запрет копирования контента группы. */
    val noCopy: Boolean = false
)
