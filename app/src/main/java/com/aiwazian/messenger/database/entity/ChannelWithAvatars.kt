/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class ChannelWithAvatars(
    @Embedded val channel: ChannelEntity,
    @Relation(
        entity = AvatarEntity::class,
        parentColumn = "id",
        entityColumn = "channelId"
    )
    val avatars: List<AvatarWithFile>
)
