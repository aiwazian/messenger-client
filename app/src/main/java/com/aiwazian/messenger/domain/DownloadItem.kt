/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import com.aiwazian.messenger.enums.DownloadStatus

data class DownloadItem(
    val id: Int,
    val fileId: String,
    val name: String,
    val size: Long,
    val progress: Int,
    val status: DownloadStatus,
    val speed: String? = null,
    val localUri: String? = null
)
