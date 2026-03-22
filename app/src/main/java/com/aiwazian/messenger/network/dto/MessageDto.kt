/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageFileDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("size") val size: String,
    @SerialName("mimeType") val mimeType: String,
    @SerialName("status") val status: String
)

@Serializable
data class MessageDto(
    @SerialName("id") val id: Int,
    @SerialName("senderId") val senderId: String,
    @SerialName("chatId") val chatId: String,
    @SerialName("text") val text: String? = null,
    @SerialName("sendTime") val sendTime: Long,
    @SerialName("editedAt") val editedAt: Long? = null,
    @SerialName("isRead") val isRead: Boolean? = null,
    @SerialName("files") val files: List<MessageFileDto> = emptyList()
)

@Serializable
data class MessageResponseDto(
    @SerialName("id") val id: Int,
    @SerialName("senderId") val senderId: String,
    @SerialName("chatId") val chatId: String,
    @SerialName("text") val text: String? = null,
    @SerialName("sendTime") val sendTime: Long,
    @SerialName("editedAt") val editedAt: Long? = null,
    @SerialName("isRead") val isRead: Boolean? = null,
    @SerialName("files") val files: List<MessageFileDto> = emptyList()
)

@Serializable
data class TextMessageRequestDto(
    @SerialName("text") val text: String
)

@Serializable
data class MediaMessageRequestDto(
    @SerialName("text") val text: String? = null,
    @SerialName("fileIds") val fileIds: List<String>
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
data class FileConfirmRequestDto(
    @SerialName("fileId") val fileId: String,
    @SerialName("text") val text: String? = null
)

@Serializable
data class FileDownloadResponseDto(
    @SerialName("downloadUrl") val downloadUrl: String,
    @SerialName("name") val name: String,
    @SerialName("size") val size: String,
    @SerialName("mimeType") val mimeType: String
)
