/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class AvatarWithFile(
    @Embedded val avatar: AvatarEntity,
    @Relation(
        parentColumn = "fileId",
        entityColumn = "id"
    )
    val file: FileEntity?
)
