/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import java.io.IOException

/**
 * Отказ, после которого повторять отправку вложения бессмысленно.
 *
 * Всё остальное — недоступный сервер, обрыв сети, истёкшая подписанная форма —
 * только откладывает следующую попытку, и сообщение остаётся «отправляется».
 * Статус ERROR ставится ровно на этих отказах.
 */
sealed class AttachmentUploadException(message: String, cause: Throwable? = null) :
    IOException(message, cause) {
    
    /**
     * Источник больше не читается: файл удалили, перенесли или отобрали доступ
     * к его content://-ссылке. Повторять нечего — читать неоткуда.
     */
    class SourceMissing(val uri: String, cause: Throwable? = null) :
        AttachmentUploadException("Attachment source $uri is gone", cause)
    
    /**
     * Файл не проходит по лимиту из подписанной сервером формы. Хранилище
     * отклонит его на любой попытке, поэтому ждать нечего.
     */
    class TooLarge(val size: Long, val limit: Long) :
        AttachmentUploadException("File size $size exceeds limit of $limit bytes")
}
