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
    val editedAt: Long? = null,
    val messageType: MessageType,
    val systemMessageEventType: SystemMessageEventType?,
    val isRead: Boolean,
    @ColumnInfo(defaultValue = "SENT") val status: MessageStatus = MessageStatus.SENT,
    val replyToId: Long? = null,
    val replyToChatId: Long? = null,
    val replyToSenderId: Long? = null,
    val replyToSenderName: String? = null,
    val replyToChatName: String? = null,
    val replyToText: String? = null,
    val replyToAttachmentTypes: String? = null,
    val forwardedFromChatId: Long? = null,
    val forwardedFromName: String? = null,
    val forwardedFromAccess: String? = null,
    @ColumnInfo(defaultValue = "0") val ownerId: Long = 0,
    @ColumnInfo(defaultValue = "0") val isEdited: Boolean = false,
    val stickerId: Long? = null,
    val stickerPackId: Long? = null,
    val stickerFileId: String? = null,
    val stickerEmojis: String? = null
)
