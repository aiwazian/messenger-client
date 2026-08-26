/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.aiwazian.messenger.enums.AttachmentType

/**
 * Вложение галереи чата в локальном кэше.
 *
 * Отдельная таблица, а не `attachment`: у той внешний ключ на `message`, и
 * положить туда вложение, не сохранив всё сообщение, нельзя. А обрывки
 * сообщений ради галереи всплыли бы в истории переписки: она читается из
 * тех же таблиц.
 *
 * Первичный ключ — [id] вложения с сервера, он же курсор страницы: повторная
 * загрузка переписывает строку, а не плодит дубли.
 */
@Entity(
    tableName = "chat_media",
    indices = [Index(value = ["ownerId", "chatId", "type"])]
)
data class ChatMediaEntity(
    @PrimaryKey
    val id: Int,
    val chatId: Long,
    val fileId: String,
    val messageId: Long,
    /** Автор сообщения: списку голосовых нужно отличать свои от чужих. */
    val senderId: Long,
    val name: String,
    val size: Long,
    val mimeType: String,
    val type: AttachmentType,
    val sendTime: Long,
    /**
     * Аккаунт, для которого строка закэширована.
     *
     * Одно устройство держит несколько аккаунтов, и у одного чата у них разный
     * состав вложений: без владельца галерея показала бы чужое.
     */
    @ColumnInfo(defaultValue = "0")
    val ownerId: Long = 0
)
