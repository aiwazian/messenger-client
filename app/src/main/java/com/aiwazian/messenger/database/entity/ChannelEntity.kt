package com.aiwazian.messenger.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("channel")
data class ChannelEntity(
    @PrimaryKey val id: Long,
    var name: String,
    var bio: String = "",
    val ownerId: Long,
    val subscribers: Int,
    val removedUser: Int,
    val channelType: Int,
    val publicLink: String?,
    val isSubscribed: Boolean = false
)