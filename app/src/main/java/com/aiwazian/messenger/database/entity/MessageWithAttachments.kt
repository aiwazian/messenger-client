/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class MessageWithAttachments(
    @Embedded val message: MessageEntity,
    @Relation(
        entity = AttachmentEntity::class,
        parentColumn = "id",
        entityColumn = "messageId"
    )
    val attachments: List<AttachmentWithFile>
)
