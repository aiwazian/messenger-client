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
 *
 * [senderId] нужен вкладке голосовых: во второй строке у своих записей стоит
 * «Вы», а у чужих — название чата.
 */
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

/** Страница галереи: items отсортированы от новых к старым. */
@Serializable
data class ChatMediaResponseDto(
    @SerialName("items") val items: List<ChatMediaItemDto> = emptyList(),
    @SerialName("nextCursorId") val nextCursorId: Int? = null
)

/**
 * Сколько вложений в чате всего.
 *
 * По всему чату, а не по загруженной странице: подпись в шапке показывает
 * итог сразу, а страницами к нему пришлось бы пролистать всю историю.
 */
@Serializable
data class ChatMediaCountsDto(
    @SerialName("photos") val photos: Int = 0,
    @SerialName("videos") val videos: Int = 0,
    @SerialName("files") val files: Int = 0,
    @SerialName("voices") val voices: Int = 0
)
