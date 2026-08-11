/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dto

import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.enums.ForwardSourceAccess
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

/**
 * Превью цитируемого сообщения. Приходит вместе с сообщением, чтобы не делать
 * отдельный запрос за оригиналом при отрисовке ответа.
 */
@Serializable
data class MessageReplyPreviewDto(
    @SerialName("id") val id: Long,
    @SerialName("senderId") val senderId: Long? = null,
    @SerialName("chatId") val chatId: Long? = null,
    @SerialName("text") val text: String? = null,
    @SerialName("messageType") val messageType: MessageType = MessageType.TEXT,
    /** Имя автора — заголовок ответа в личном чате. */
    @SerialName("senderName") val senderName: String? = null,
    /** Название группы/канала — заголовок ответа в группе или канале. */
    @SerialName("chatName") val chatName: String? = null,
    /** Нужны, чтобы вместо пустого текста показать «Фото», «Видео», «Голосовое сообщение». */
    @SerialName("attachmentTypes") val attachmentTypes: List<AttachmentType> = emptyList()
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
    @SerialName("readInfo") val readInfo: List<MessageReadInfoDto>? = null,
    @SerialName("replyToId") val replyToId: Long? = null,
    @SerialName("replyToChatId") val replyToChatId: Long? = null,
    @SerialName("replyTo") val replyTo: MessageReplyPreviewDto? = null,
    /** Автор контента, а не последний пересылавший. */
    @SerialName("forwardedFromChatId") val forwardedFromChatId: Long? = null,
    @SerialName("forwardedFromName") val forwardedFromName: String? = null,
    @SerialName("forwardedFromAccess") val forwardedFromAccess: ForwardSourceAccess? = null
)

@Serializable
data class TextMessageRequestDto(
    @SerialName("text") val text: String,
    /** id сообщения, на которое отвечаем. Сервер ждёт строку из-за BigInt. */
    @SerialName("replyToId") val replyToId: String? = null
)

@Serializable
data class EditMessageRequestDto(
    @SerialName("text") val text: String
)

@Serializable
data class FileInitRequestDto(
    @SerialName("name") val name: String,
    @SerialName("size") val size: Long,
    @SerialName("mimeType") val mimeType: String,
    /**
     * Что именно мы собираемся отправить.
     *
     * Сервер зашивает категорию в политику загрузки, поэтому подменить картинку
     * на исполняемый файл уже не выйдет: S3 отклонит запрос по Content-Type.
     * Для аватарок категорию проставляет сам сервер, клиенту её слать не нужно.
     */
    @SerialName("category") val category: AttachmentType = AttachmentType.FILE
)

/**
 * Форма загрузки, подписанная сервером (S3 presigned POST).
 *
 * [fields] нужно положить в multipart-запрос до части с файлом — в этом порядке
 * S3 проверяет политику. Поле файла всегда называется "file".
 */
@Serializable
data class FileInitResponseDto(
    @SerialName("url") val url: String,
    @SerialName("fields") val fields: Map<String, String> = emptyMap(),
    @SerialName("fileId") val fileId: String,
    /** Максимально допустимый размер, чтобы не гнать заведомо отклоняемый файл. */
    @SerialName("maxSizeBytes") val maxSizeBytes: Long = 0
)

@Serializable
data class AttachmentInputDto(
    @SerialName("fileId") val fileId: String,
    @SerialName("type") val type: AttachmentType
)

@Serializable
data class FileConfirmRequestDto(
    @SerialName("attachments") val attachments: List<AttachmentInputDto>,
    @SerialName("text") val text: String? = null,
    @SerialName("replyToId") val replyToId: String? = null
)

/** Пересылка одного сообщения в несколько чатов за один запрос. */
@Serializable
data class ForwardMessageRequestDto(
    @SerialName("targetChatIds") val targetChatIds: List<String>
)

@Serializable
data class FileDownloadResponseDto(
    @SerialName("downloadUrl") val downloadUrl: String,
    @SerialName("name") val name: String,
    @SerialName("size") val size: Long,
    @SerialName("mimeType") val mimeType: String
)
