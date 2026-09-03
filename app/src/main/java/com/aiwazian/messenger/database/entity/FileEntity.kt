/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.aiwazian.messenger.enums.DownloadStatus

@Entity("file")
data class FileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val size: Long,
    val path: String?,
    val status: DownloadStatus,
    /**
     * Размеры кадра в пикселях — только у фото и видео.
     *
     * Лежат рядом с файлом, а не со вложением: пересланная копия ссылается
     * на тот же файл, и размеры ей достаются сразу.
     *
     * Пусты у всего, что не фото и не видео, и у вложений, скачанных до
     * этого изменения: серверу неоткуда было их взять.
     */
    val width: Int? = null,
    val height: Int? = null
)
