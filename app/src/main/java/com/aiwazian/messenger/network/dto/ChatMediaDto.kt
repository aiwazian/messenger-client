/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dto

import com.aiwazian.messenger.enums.AttachmentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Вложение из галереи чата.
 *
 * Приходит без самого сообщения: галерее нужны только файл и время отправки,
 * а [messageId] — чтобы взять ссылку на скачивание: она выдаётся по паре
 * сообщение + файл.
 */
@Serializable
data class ChatMediaItemDto(
    @SerialName("id") val id: Int,
    @SerialName("fileId") val fileId: String,
    @SerialName("messageId") val messageId: Long,
    @SerialName("name") val name: String = "",
    @SerialName("size") val size: Long = 0,
    @SerialName("mimeType") val mimeType: String = "",
    @SerialName("type") val type: AttachmentType = AttachmentType.FILE,
    @SerialName("sendTime") val sendTime: Long = 0
)

/** Страница галереи: items отсортированы от новых к старым. */
@Serializable
data class ChatMediaResponseDto(
    @SerialName("items") val items: List<ChatMediaItemDto> = emptyList(),
    @SerialName("nextCursorId") val nextCursorId: Int? = null
)
