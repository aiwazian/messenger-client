/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import android.net.Uri
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.DownloadStatus

data class MessageAttachment(
    val fileId: String,
    val messageId: Long,
    val name: String,
    val size: Long,
    val extension: String,
    val status: DownloadStatus,
    val progress: Int,
    val localUri: Uri?,
    val type: AttachmentType,
    val sortOrder: Int,
    /**
     * Ширина и высота кадра в пикселях — только у фото и видео.
     *
     * Нужны до того, как картинка загружена: по ним пузырёк считает высоту
     * карточки и держит место нужной формы. Без них под любое вложение
     * отводится один и тот же прямоугольник, и лента прыгает в момент,
     * когда картинка пришла и оказалась другой формы.
     *
     * Пусты у документов и голосовых, а также у медиа, отправленного старым
     * клиентом или до этой правки: такие вложения рисуются по прежнему
     * правилу.
     */
    val width: Int? = null,
    val height: Int? = null
)
