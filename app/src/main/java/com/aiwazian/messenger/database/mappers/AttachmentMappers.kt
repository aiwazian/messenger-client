package com.aiwazian.messenger.database.mappers

import com.aiwazian.messenger.data.Attachment
import com.aiwazian.messenger.database.entity.AttachmentEntity

fun Attachment.toEntity(): AttachmentEntity {
    return AttachmentEntity(
        id = this.id,
        messageId = this.messageId,
        name = this.name,
        url = this.url,
        size = this.size
    )
}

fun AttachmentEntity.toModel(): Attachment {
    return Attachment(
        id = this.id,
        messageId = this.messageId,
        name = this.name,
        url = this.url,
        size = this.size
    )
}
