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
    val status: DownloadStatus = DownloadStatus.IDLE,
    val progress: Int = 0,
    val localUri: Uri? = null
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
