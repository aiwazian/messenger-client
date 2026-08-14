/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.aiwazian.messenger.enums.MessageStatus
import com.aiwazian.messenger.enums.MessageType
import com.aiwazian.messenger.enums.SystemMessageEventType

@Entity(
    tableName = "message",
    indices = [Index(value = ["ownerId", "chatId"])]
)
data class MessageEntity(
    @PrimaryKey val id: Long,
    val senderId: Long,
    val chatId: Long,
    val text: String?,
    val sendTime: Long,
    /**
     * Когда сообщение правили.
     *
     * Сервер хранит это время в Redis трое суток и потом отдаёт пустое поле,
     * поэтому сам факт правки лежит в [isEdited], а не выводится отсюда.
     */
    val editedAt: Long? = null,
    val messageType: MessageType,
    val systemMessageEventType: SystemMessageEventType?,
    val isRead: Boolean,
    @ColumnInfo(defaultValue = "SENT") val status: MessageStatus = MessageStatus.SENT,
    
    // region Ответ на сообщение
    /** id цитируемого сообщения — по нему делается прыжок к оригиналу. */
    val replyToId: Long? = null,
    /** Чат оригинала: ответить можно на сообщение из другого чата. */
    val replyToChatId: Long? = null,
    val replyToSenderId: Long? = null,
    /** Готовое превью, чтобы отрисовать ответ без загрузки оригинала. */
    val replyToSenderName: String? = null,
    val replyToChatName: String? = null,
    val replyToText: String? = null,
    /** Типы вложений оригинала через запятую: IMAGE,VIDEO. */
    val replyToAttachmentTypes: String? = null,
    // endregion
    
    // region Пересылка
    /** Чат владельца контента, а не посредника. */
    val forwardedFromChatId: Long? = null,
    val forwardedFromName: String? = null,
    /** Имя ForwardSourceAccess; хранится строкой, чтобы не плодить конвертеры Room. */
    val forwardedFromAccess: String? = null,
    // endregion
    
    /**
     * Аккаунт, которому принадлежит эта копия сообщения.
     *
     * На устройстве живёт несколько аккаунтов, и у одного и того же сообщения isRead
     * у них разное: в группе «прочитано» относится к конкретному читателю, а не к
     * сообщению. Общая на всех строка давала чужие галочки, чужие непрочитанные и
     * чистку кэша одного аккаунта вместе с кэшом другого.
     *
     * 0 — владелец ещё не присвоен: его проставляет MessageDao.saveMessages по
     * таблице account. Поле добавлено в конец списка, чтобы не ломать позиционные
     * вызовы конструктора.
     */
    @ColumnInfo(defaultValue = "0") val ownerId: Long = 0,
    
    /**
     * Сообщение правили хотя бы раз.
     *
     * Флаг приходит с сервера всегда, а [editedAt] — только трое суток после
     * правки. Без отдельной колонки подпись «изменено» слетала бы с сообщения при
     * первой же перезакачке истории на четвёртый день.
     */
    @ColumnInfo(defaultValue = "0") val isEdited: Boolean = false
)
