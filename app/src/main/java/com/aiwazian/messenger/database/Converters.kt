/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database

import androidx.room.TypeConverter
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.ChannelType
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.enums.GroupType
import com.aiwazian.messenger.enums.MessageType

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
    
    @TypeConverter
    fun fromChannelType(type: ChannelType): String = type.name
    
    @TypeConverter
    fun toChannelType(type: String): ChannelType = try {
        ChannelType.valueOf(type)
    } catch (_: Exception) {
        ChannelType.PRIVATE
    }
    
    @TypeConverter
    fun fromGroupType(type: GroupType): String = type.name
    
    @TypeConverter
    fun toGroupType(type: String): GroupType = try {
        GroupType.valueOf(type)
    } catch (_: Exception) {
        GroupType.PRIVATE
    }
    
    @TypeConverter
    fun fromMessageType(type: MessageType): String = type.name
    
    @TypeConverter
    fun toMessageType(type: String): MessageType = try {
        MessageType.valueOf(type)
    } catch (_: Exception) {
        MessageType.TEXT
    }
}
