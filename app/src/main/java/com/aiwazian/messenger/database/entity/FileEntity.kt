/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aiwazian.messenger.enums.DownloadStatus

@Entity("file")
data class FileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val size: Long,
    val path: String?,
    val status: DownloadStatus
)
