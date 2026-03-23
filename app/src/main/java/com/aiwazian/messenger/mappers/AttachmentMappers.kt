/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.database.entity.AttachmentEntity
import com.aiwazian.messenger.domain.MessageFile
import com.aiwazian.messenger.enums.AttachmentType

fun AttachmentEntity.toDomain(): MessageFile = MessageFile(
    id = this.id,
    name = this.name,
    size = this.size,
    extension = this.extension,
    status = this.status,
    progress = this.progress,
    localUri = this.localUri
)

fun MessageFile.toEntity(relationId: Long, type: AttachmentType, chatId: Long? = null): AttachmentEntity = AttachmentEntity(
    id = this.id,
    relationId = relationId,
    chatId = chatId,
    type = type,
    name = this.name,
    size = this.size,
    extension = this.extension,
    status = this.status,
    progress = this.progress,
    localUri = this.localUri
)
