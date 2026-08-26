/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import android.net.Uri
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.DownloadStatus

/**
 * Вложение галереи чата.
 *
 * Почти то же, что [MessageAttachment], но без привязки к загруженному окну
 * истории: галерея живёт своим списком и знает про сообщение только его id —
 * ровно столько, сколько нужно для ссылки на скачивание.
 *
 * [status] и [localUri] берутся из локального кэша файлов, а не с сервера:
 * уже скачанное показывается сразу и повторно не качается.
 */
data class ChatMediaItem(
    val id: Int,
    val fileId: String,
    val messageId: Long,
    val name: String,
    val size: Long,
    val mimeType: String,
    val type: AttachmentType,
    val sendTime: Long,
    /** Автор сообщения: список голосовых отличает свои записи от чужих. */
    val senderId: Long = 0,
    val status: DownloadStatus = DownloadStatus.IDLE,
    val progress: Int = 0,
    val localUri: Uri? = null,
    /**
     * Длина голосового в миллисекундах, если она уже известна.
     *
     * Сервер её не отдаёт: она считается из скачанного файла и ложится в
     * кэш, поэтому при первом показе может быть пустой.
     */
    val durationMs: Int? = null
) {
    /** Расширение для иконки документа: сервер его отдельно не отдаёт. */
    val extension: String get() = name.substringAfterLast('.', "")
    
    /** Открывается во весь экран, а не скачивается документом. */
    val isVisual: Boolean
        get() = type == AttachmentType.IMAGE || type == AttachmentType.VIDEO || type == AttachmentType.GIF
}

/** Страница галереи вместе с курсором следующей. */
data class ChatMediaPage(
    val items: List<ChatMediaItem>,
    val nextCursorId: Int? = null
)

/**
 * Сколько вложений в чате всего.
 *
 * Фото и видео разделены: они лежат на одной вкладке, но в подписи
 * перечисляются по отдельности.
 */
data class ChatMediaCounts(
    val photos: Int = 0,
    val videos: Int = 0,
    val files: Int = 0,
    val voices: Int = 0
)
