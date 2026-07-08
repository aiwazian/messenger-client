/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room3.Embedded
import androidx.room3.Relation

data class ChannelWithAvatars(
    @Embedded val channel: ChannelEntity,
    @Relation(
        entity = AvatarEntity::class,
        parentColumns = ["id"],
        entityColumns = ["channelId"]
    )
    val avatars: List<AvatarWithFile>
)
