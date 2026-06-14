/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dto

import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.enums.MessageType
import com.aiwazian.messenger.enums.SystemMessageEventType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageReadInfoDto(
    @SerialName("userId") val userId: Long,
    @SerialName("firstName") val firstName: String = "",
    @SerialName("lastName") val lastName: String? = null,
    @SerialName("readAt") val readAt: Long
)

@Serializable
data class MessageAttachmentDto(
    @SerialName("fileId") val fileId: String,
    @SerialName("name") val name: String = "",
    @SerialName("size") val size: Long = 0,
    @SerialName("mimeType") val mimeType: String = "",
    @SerialName("status") val status: DownloadStatus = DownloadStatus.UPLOADED,
    @SerialName("type") val type: AttachmentType = AttachmentType.FILE,
    @SerialName("sortOrder") val sortOrder: Int = 0,
)

@Serializable
data class MessageDto(
    @SerialName("id") val id: Long,
    @SerialName("senderId") val senderId: Long,
    @SerialName("chatId") val chatId: Long,
    @SerialName("text") val text: String? = null,
    @SerialName("sendTime") val sendTime: Long,
    @SerialName("editedAt") val editedAt: Long? = null,
    @SerialName("isRead") val isRead: Boolean? = null,
    @SerialName("messageType") val messageType: MessageType = MessageType.TEXT,
    @SerialName("systemEventType") val systemEventType: SystemMessageEventType? = null,
    @SerialName("attachments") val attachments: List<MessageAttachmentDto> = emptyList(),
    @SerialName("readInfo") val readInfo: List<MessageReadInfoDto>? = null
)

@Serializable
data class TextMessageRequestDto(
    @SerialName("text") val text: String
)

@Serializable
data class FileInitRequestDto(
    @SerialName("name") val name: String,
    @SerialName("size") val size: Long,
    @SerialName("mimeType") val mimeType: String
)

@Serializable
data class FileInitResponseDto(
    @SerialName("signedUrl") val signedUrl: String,
    @SerialName("fileId") val fileId: String
)

@Serializable
data class AttachmentInputDto(
    @SerialName("fileId") val fileId: String,
    @SerialName("type") val type: AttachmentType
)

@Serializable
data class FileConfirmRequestDto(
    @SerialName("attachments") val attachments: List<AttachmentInputDto>,
    @SerialName("text") val text: String? = null
)

@Serializable
data class FileDownloadResponseDto(
    @SerialName("downloadUrl") val downloadUrl: String,
    @SerialName("name") val name: String,
    @SerialName("size") val size: Long,
    @SerialName("mimeType") val mimeType: String
)
