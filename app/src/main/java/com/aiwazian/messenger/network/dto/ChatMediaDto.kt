/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dto

import com.aiwazian.messenger.enums.AttachmentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class ChatMediaItemDto(
    @SerialName("id") val id: Int,
    @SerialName("fileId") val fileId: String,
    @SerialName("messageId") val messageId: Long,
    @SerialName("senderId") val senderId: Long = 0,
    @SerialName("name") val name: String = "",
    @SerialName("size") val size: Long = 0,
    @SerialName("mimeType") val mimeType: String = "",
    @SerialName("type") val type: AttachmentType = AttachmentType.FILE,
    @SerialName("sendTime") val sendTime: Long = 0
)


@Serializable
data class ChatMediaResponseDto(
    @SerialName("items") val items: List<ChatMediaItemDto> = emptyList(),
    @SerialName("nextCursorId") val nextCursorId: Int? = null
)


@Serializable
data class ChatMediaCountsDto(
    @SerialName("photos") val photos: Int = 0,
    @SerialName("videos") val videos: Int = 0,
    @SerialName("files") val files: Int = 0,
    @SerialName("music") val music: Int = 0,
    @SerialName("voices") val voices: Int = 0,
    @SerialName("contacts") val contacts: Int = 0
)


@Serializable
data class ChatContactItemDto(
    @SerialName("contactId") val contactId: Long,
    @SerialName("messageId") val messageId: Long,
    @SerialName("senderId") val senderId: Long = 0,
    @SerialName("sendTime") val sendTime: Long = 0,
    @SerialName("name") val name: String = "",
    @SerialName("phones") val phones: List<String> = emptyList(),
    @SerialName("emails") val emails: List<String> = emptyList(),
    @SerialName("urls") val urls: List<String> = emptyList(),
    @SerialName("addresses") val addresses: List<String> = emptyList(),
    @SerialName("organization") val organization: String? = null,
    @SerialName("jobTitle") val jobTitle: String? = null,
    @SerialName("birthday") val birthday: String? = null
)


@Serializable
data class ChatContactsResponseDto(
    @SerialName("items") val items: List<ChatContactItemDto> = emptyList(),
    @SerialName("nextCursorId") val nextCursorId: Int? = null
)
