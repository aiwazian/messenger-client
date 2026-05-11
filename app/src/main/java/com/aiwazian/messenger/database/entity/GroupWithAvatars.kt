/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class GroupWithAvatars(
    @Embedded val group: GroupEntity,
    @Relation(
        entity = AvatarEntity::class,
        parentColumn = "id",
        entityColumn = "groupId"
    )
    val avatars: List<AvatarWithFile>
)
