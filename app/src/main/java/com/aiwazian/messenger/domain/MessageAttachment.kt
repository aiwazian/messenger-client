/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import android.net.Uri
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.DownloadStatus

data class MessageAttachment(
    val fileId: String,
    val messageId: Long,
    val name: String,
    val size: Long,
    val extension: String,
    val status: DownloadStatus,
    val progress: Int,
    val localUri: Uri?,
    val type: AttachmentType,
    val sortOrder: Int
)
