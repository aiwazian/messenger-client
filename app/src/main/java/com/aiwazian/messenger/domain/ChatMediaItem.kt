/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import android.net.Uri
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.DownloadStatus

data class ChatMediaItem(
    val id: Int,
    val fileId: String,
    val messageId: Long,
    val name: String,
    val size: Long,
    val mimeType: String,
    val type: AttachmentType,
    val sendTime: Long,
    val senderId: Long = 0,
    val status: DownloadStatus = DownloadStatus.IDLE,
    val progress: Int = 0,
    val localUri: Uri? = null,
    val durationMs: Int? = null
) {
    val extension: String get() = name.substringAfterLast('.', "")
}

data class ChatMediaPage(
    val items: List<ChatMediaItem>,
    val nextCursorId: Int? = null
)

data class ChatMediaCounts(
    val photos: Int = 0,
    val videos: Int = 0,
    val files: Int = 0,
    val voices: Int = 0
)
