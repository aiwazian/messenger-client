/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room3.Embedded
import androidx.room3.Relation

data class UserWithAvatars(
    @Embedded val user: UserEntity,
    @Relation(
        entity = AvatarEntity::class,
        parentColumns = ["id"],
        entityColumns = ["userId"]
    )
    val avatars: List<AvatarWithFile>
)
