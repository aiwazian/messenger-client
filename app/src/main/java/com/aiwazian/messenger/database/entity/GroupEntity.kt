package com.aiwazian.messenger.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("group")
data class GroupEntity(
    @PrimaryKey val id: Long,
    val ownerId: Long,
    var name: String,
    var bio: String,
    val members: Int
)