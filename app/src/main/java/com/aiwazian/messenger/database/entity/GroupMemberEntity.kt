/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "group_member",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["groupId"]),
        Index(value = ["userId"]),
        Index(value = ["groupId", "userId"], unique = true)
    ]
)
data class GroupMemberEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val groupId: Long,
    val userId: Long
)

@Entity(
    tableName = "group_blacklist",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["groupId"]),
        Index(value = ["userId"]),
        Index(value = ["userId", "groupId"], unique = true)
    ]
)
data class GroupBlackListEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val groupId: Long,
    val userId: Long
)
