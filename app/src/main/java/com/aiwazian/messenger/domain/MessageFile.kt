/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import com.aiwazian.messenger.enums.DownloadStatus
import kotlinx.serialization.Serializable

@Serializable
data class MessageFile(
    val id: String,
    val name: String,
    val size: Long,
    val extension: String,
    val status: DownloadStatus = DownloadStatus.IDLE,
    val progress: Int = 0,
    val localUri: String? = null
)
