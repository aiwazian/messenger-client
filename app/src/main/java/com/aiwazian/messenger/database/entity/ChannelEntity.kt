/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aiwazian.messenger.enums.ChannelType

@Entity("channel")
data class ChannelEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val bio: String?,
    val ownerId: Long?,
    val channelType: ChannelType,
    val subscribers: Int,
    val username: String?,
    val isSubscribed: Boolean
)
