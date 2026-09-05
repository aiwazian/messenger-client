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

data class MessageReplyPreview(
    val messageId: Long,
    val chatId: Long? = null,
    val senderId: Long? = null,
    val senderName: String? = null,
    val chatName: String? = null,
    val text: String? = null,
    val attachmentTypes: List<AttachmentType> = emptyList()
) {
    val title: String? get() = chatName ?: senderName
}

data class ForwardedFrom(
    val chatId: Long,
    val name: String,
    val access: ForwardSourceAccess
)

data class MessageSticker(
    val id: Long,
    val packId: Long,
    val fileId: String,
    val emojis: List<String> = emptyList()
)

data class Message(
    val id: Long,
    val senderId: Long,
    val chatId: Long,
    val text: String?,
    val sendTime: Long,
    val editedAt: Long? = null,
    val isRead: Boolean,
    val status: MessageStatus = MessageStatus.SENT,
    val messageType: MessageType,
    val systemMessageEventType: SystemMessageEventType?,
    val attachments: List<MessageAttachment>,
    val readInfo: List<MessageReadInfo>? = null,
    val replyTo: MessageReplyPreview? = null,
    val forwardedFrom: ForwardedFrom? = null,
    val isEdited: Boolean = false,
    val sticker: MessageSticker? = null
)
