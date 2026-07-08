/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room3.Embedded
import androidx.room3.Relation

data class AttachmentWithFile(
    @Embedded val attachment: AttachmentEntity,
    @Relation(
        parentColumns = ["fileId"],
        entityColumns = ["id"]
    )
    val file: FileEntity
)
