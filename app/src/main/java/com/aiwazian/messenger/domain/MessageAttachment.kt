/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.DownloadStatus

data class MessageAttachment(
    val id: String,
    val name: String,
    val size: Long,
    val extension: String,
    val status: DownloadStatus,
    val progress: Int,
    val localUri: String?,
    val type: AttachmentType
)
