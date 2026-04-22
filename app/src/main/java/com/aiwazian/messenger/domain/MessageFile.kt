/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import com.aiwazian.messenger.enums.DownloadStatus

data class MessageFile(
    val id: String,
    val name: String,
    val size: Long,
    val extension: String,
    val status: DownloadStatus,
    val progress: Int,
    val localUri: String?
)
