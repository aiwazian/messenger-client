/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class AttachmentWithFile(
    @Embedded val attachment: AttachmentEntity,
    @Relation(
        parentColumn = "fileId",
        entityColumn = "id"
    )
    val file: FileEntity
)
