/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "avatars")
data class AvatarEntity(
    @PrimaryKey val fileId: String,
    val userId: Long? = null,
    val channelId: Long? = null,
    val groupId: Long? = null,
    val sortOrder: Int
)
