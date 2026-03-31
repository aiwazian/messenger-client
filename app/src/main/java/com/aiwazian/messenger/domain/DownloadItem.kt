/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import com.aiwazian.messenger.enums.DownloadStatus

data class DownloadItem(
    val id: Int,
    val messageId: Int? = null,
    val fileId: String? = null,
    val name: String,
    val size: Long,
    val progress: Int,
    val status: DownloadStatus,
    val isUpload: Boolean = false,
    val speed: String? = null,
    val localUri: String? = null
)
