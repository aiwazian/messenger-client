/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.aiwazian.messenger.enums.DownloadStatus

@Entity("file")
data class FileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val size: Long,
    val path: String?,
    val status: DownloadStatus
)
