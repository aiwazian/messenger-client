/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database

import androidx.room3.ColumnTypeConverter
import com.aiwazian.messenger.domain.MessageContact
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.ChannelType
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.enums.GroupType
import com.aiwazian.messenger.enums.MessageType
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @ColumnTypeConverter
    fun toMessageContact(value: String?): MessageContact? = value?.let { raw ->
        runCatching { json.decodeFromString<MessageContact>(raw) }.getOrNull()
    }

    @ColumnTypeConverter
    fun fromMessageContact(contact: MessageContact?): String? = contact?.let { raw ->
        json.encodeToString(MessageContact.serializer(), raw)
    }

    @ColumnTypeConverter
    fun toDownloadStatus(status: String): DownloadStatus = try {
        DownloadStatus.valueOf(status)
    } catch (_: Exception) {
        DownloadStatus.IDLE
    }
    
    @ColumnTypeConverter
    fun toAttachmentType(type: String): AttachmentType = try {
        AttachmentType.valueOf(type)
    } catch (_: Exception) {
        AttachmentType.FILE
    }
    
    @ColumnTypeConverter
    fun toChannelType(type: String): ChannelType = try {
        ChannelType.valueOf(type)
    } catch (_: Exception) {
        ChannelType.PRIVATE
    }
    
    @ColumnTypeConverter
    fun toGroupType(type: String): GroupType = try {
        GroupType.valueOf(type)
    } catch (_: Exception) {
        GroupType.PRIVATE
    }
    
    @ColumnTypeConverter
    fun toMessageType(type: String): MessageType = try {
        MessageType.valueOf(type)
    } catch (_: Exception) {
        MessageType.TEXT
    }
}
