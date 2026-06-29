/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("user")
data class UserEntity(
    @PrimaryKey val id: Long,
    val firstName: String = "",
    val lastName: String? = null,
    val username: String? = null,
    val bio: String? = null,
    val dateOfBirth: Long? = null,
    val lastSeen: Long? = null,
    val profileChannelId: Long? = null,
    @ColumnInfo(defaultValue = "false") val isBlocked: Boolean = false,
    @ColumnInfo(defaultValue = "false") val isBlockedByThem: Boolean = false
)
