/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import kotlinx.serialization.Serializable

@Serializable
enum class DownloadStatus {
    IDLE,
    DOWNLOADING,
    UPLOADING,
    PAUSED,
    COMPLETED,
    CANCELLED,
    FAILED
}

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

data class Message(
    val id: Int = 0,
    val senderId: Long = 0,
    val chatId: Long = 0,
    val text: String? = null,
    val sendTime: Long = 0,
    val isRead: Boolean = false,
    val files: List<MessageFile> = emptyList()
)
