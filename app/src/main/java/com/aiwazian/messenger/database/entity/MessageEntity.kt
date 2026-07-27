/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.aiwazian.messenger.enums.MessageStatus
import com.aiwazian.messenger.enums.MessageType
import com.aiwazian.messenger.enums.SystemMessageEventType

@Entity("message")
data class MessageEntity(
    @PrimaryKey val id: Long,
    val senderId: Long,
    val chatId: Long,
    val text: String?,
    val sendTime: Long,
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
    val forwardedFromAccess: String? = null
    // endregion
)
