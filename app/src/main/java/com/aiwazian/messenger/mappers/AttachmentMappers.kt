/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.database.entity.AttachmentEntity
import com.aiwazian.messenger.domain.MessageAttachment

fun AttachmentEntity.toDomain(): MessageAttachment = MessageAttachment(
    id = id,
    name = name,
    size = size,
    extension = extension,
    status = status,
    progress = progress,
    localUri = localUri,
    type = type
)

fun MessageAttachment.toEntity(relationId: Long, chatId: Long? = null): AttachmentEntity = AttachmentEntity(
    id = id,
    relationId = relationId,
    chatId = chatId,
    type = type,
    name = name,
    size = size,
    extension = extension,
    status = status,
    progress = progress,
    localUri = localUri
)
