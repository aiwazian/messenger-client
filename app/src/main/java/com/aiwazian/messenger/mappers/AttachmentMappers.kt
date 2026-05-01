/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.mappers

import androidx.core.net.toUri
import com.aiwazian.messenger.database.entity.AttachmentEntity
import com.aiwazian.messenger.database.entity.AttachmentWithFile
import com.aiwazian.messenger.database.entity.FileEntity
import com.aiwazian.messenger.domain.MessageAttachment

fun AttachmentEntity.toDomain(file: FileEntity) = MessageAttachment(
    fileId = file.id,
    messageId = messageId,
    name = file.name,
    size = file.size,
    extension = file.name.substringAfterLast('.'),
    status = file.status,
    progress = 0,
    localUri = file.path?.toUri(),
    type = type,
    sortOrder = sortOrder
)

fun AttachmentWithFile.toDomain() = attachment.toDomain(file)

fun MessageAttachment.toEntity(file: FileEntity) = AttachmentEntity(
    fileId = file.id,
    messageId = messageId,
    type = type,
    sortOrder = sortOrder
)