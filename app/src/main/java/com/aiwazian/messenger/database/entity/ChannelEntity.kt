/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.aiwazian.messenger.enums.ChannelType

@Entity("channel")
data class ChannelEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val bio: String?,
    val ownerId: Long?,
    val channelType: ChannelType,
    val subscribers: Int,
    val removedUsers: Int?,
    val username: String?,
    val isSubscribed: Boolean,
    /** Запрет копирования контента канала. */
    val noCopy: Boolean = false
)
