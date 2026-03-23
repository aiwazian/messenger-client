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
        // filters by type are tricky in Relation without custom Query in DAO
    )
    val allAttachments: List<AttachmentEntity>
) {
    // Filter attachments by type
    val messageAttachments: List<AttachmentEntity>
        get() = allAttachments.filter { it.type == AttachmentType.MESSAGE }
}
