/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class UserWithAvatars(
    @Embedded val user: UserEntity,
    @Relation(
        entity = AvatarEntity::class,
        parentColumn = "id",
        entityColumn = "userId"
    )
    val avatars: List<AvatarWithFile>
)
