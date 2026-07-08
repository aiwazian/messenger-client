/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room3.Embedded
import androidx.room3.Relation

data class GroupWithAvatars(
    @Embedded val group: GroupEntity,
    @Relation(
        entity = AvatarEntity::class,
        parentColumns = ["id"],
        entityColumns = ["groupId"]
    )
    val avatars: List<AvatarWithFile>
)
