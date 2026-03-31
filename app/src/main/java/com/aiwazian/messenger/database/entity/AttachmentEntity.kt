/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.DownloadStatus

@Entity("attachment")
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val relationId: Long,
    val chatId: Long? = null,
    val type: AttachmentType,
    val name: String,
    val size: Long,
    val extension: String,
    val status: DownloadStatus = DownloadStatus.IDLE,
    val progress: Int = 0,
    val localUri: String? = null
)
