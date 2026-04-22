/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room.Embedded
import androidx.room.Relation
import com.aiwazian.messenger.enums.AttachmentType

data class MessageWithAttachments(
    @Embedded val message: MessageEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "relationId",
    )
    val allAttachments: List<AttachmentEntity>
) {
    val messageAttachments: List<AttachmentEntity>
        get() = allAttachments.filter { it.type == AttachmentType.FILE }
}
