/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.ForwardSourceAccess
import com.aiwazian.messenger.enums.MessageStatus
import com.aiwazian.messenger.enums.MessageType
import com.aiwazian.messenger.enums.SystemMessageEventType

data class MessageReadInfo(
    val userId: Long,
    val firstName: String,
    val lastName: String?,
    val readAt: Long
)

/**
 * Превью сообщения, на которое ответили.
 *
 * Оригинал может быть далеко за пределами окна истории или вообще в другом чате,
 * поэтому текст и подписи приходят вместе с ответом.
 */
data class MessageReplyPreview(
    val messageId: Long,
    val chatId: Long? = null,
    val senderId: Long? = null,
    val senderName: String? = null,
    val chatName: String? = null,
    val text: String? = null,
    val attachmentTypes: List<AttachmentType> = emptyList()
) {
    /** Заголовок ответа: название канала или группы, в личном чате — имя автора. */
    val title: String? get() = chatName ?: senderName
}

/** Источник пересылки: всегда владелец контента, а не посредник. */
data class ForwardedFrom(
    val chatId: Long,
    val name: String,
    val access: ForwardSourceAccess
)

data class Message(
    val id: Long,
    val senderId: Long,
    val chatId: Long,
    val text: String?,
    val sendTime: Long,
    /** Когда именно правили. Известно только трое суток после правки. */
    val editedAt: Long? = null,
    val isRead: Boolean,
    val status: MessageStatus = MessageStatus.SENT,
    val messageType: MessageType,
    val systemMessageEventType: SystemMessageEventType?,
    val attachments: List<MessageAttachment>,
    val readInfo: List<MessageReadInfo>? = null,
    val replyTo: MessageReplyPreview? = null,
    val forwardedFrom: ForwardedFrom? = null,
    /**
     * Сообщение правили хотя бы раз.
     *
     * Отдельный флаг нужен, потому что editedAt пропадает через трое суток, а
     * подпись «изменено» у сообщения должна оставаться навсегда. Поле добавлено
     * в конец, чтобы не ломать позиционные вызовы конструктора.
     */
    val isEdited: Boolean = false
)
