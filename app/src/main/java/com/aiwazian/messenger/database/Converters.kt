/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database

import androidx.room.TypeConverter
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.DownloadStatus

class Converters {
    @TypeConverter
    fun fromDownloadStatus(status: DownloadStatus): String = status.name

    @TypeConverter
    fun toDownloadStatus(status: String): DownloadStatus = try {
        DownloadStatus.valueOf(status)
    } catch (_: Exception) {
        DownloadStatus.IDLE
    }

    @TypeConverter
    fun fromAttachmentType(type: AttachmentType): String = type.name

    @TypeConverter
    fun toAttachmentType(type: String): AttachmentType = try {
        AttachmentType.valueOf(type)
    } catch (_: Exception) {
        AttachmentType.FILE
    }
}
