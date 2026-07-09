/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Refresh
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.domain.MessageReadInfo
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.enums.MessageStatus
import com.aiwazian.messenger.enums.MessageType
import com.aiwazian.messenger.enums.SystemMessageEventType
import com.aiwazian.messenger.extensions.getFileType
import com.aiwazian.messenger.extensions.toInstance
import com.aiwazian.messenger.extensions.toPrettyTime
import com.aiwazian.messenger.ui.components.topBar.DropdownMenuAction
import com.aiwazian.messenger.utils.UiText
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

class ChatItemMapper(
    private val context: Context,
    private val myId: Long,
    private val chatId: Long,
    private val isOwner: Boolean,
    private val userNamesCache: Map<Long, String>,
    private val groupReadInfo: Map<Long, List<MessageReadInfo>>,
    private val onCopyText: (String) -> Unit,
    private val onEditMessage: (Message) -> Unit,
    private val onDeleteMessage: (Message) -> Unit,
    private val onRetrySendMessage: (Message) -> Unit,
    private val onCancelSendMessage: (Message) -> Unit,
    private val onLoadUserName: (Long) -> Unit
) {
    fun map(messages: List<Message>): List<ChatItem> {
        val chatItems = mutableListOf<ChatItem>()
        var lastDate: java.time.LocalDate? = null
        var lastSenderId: Long? = null
        
        val chatType = ChatType.fromId(chatId)
        
        messages.forEach { message ->
            val messageDate =
                message.sendTime.toInstance().atZone(ZoneId.systemDefault()).toLocalDate()
            
            if (lastDate == null || !messageDate.isEqual(lastDate)) {
                val monthName = messageDate.month.getDisplayName(
                    TextStyle.FULL, Locale.getDefault()
                )
                val capitalizedMonthName = monthName.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase() else it.toString()
                }
                chatItems.add(ChatItem.DateSeparator("${messageDate.dayOfMonth} $capitalizedMonthName"))
                lastDate = messageDate
            }
            
            if (message.messageType == MessageType.SYSTEM && message.systemMessageEventType != null) {
                val textResId = when (message.systemMessageEventType) {
                    SystemMessageEventType.CHANNEL_CREATED -> R.string.channel_created
                    SystemMessageEventType.GROUP_CREATED -> R.string.group_created
                    SystemMessageEventType.HISTORY_CLEARED -> R.string.history_cleared
                }
                chatItems.add(
                    ChatItem.SystemMessage(
                        text = UiText.StringResource(resId = textResId),
                        sendTime = message.sendTime
                    )
                )
                return@forEach
            }
            
            val isMine = message.senderId == myId && chatType != ChatType.CHANNEL
            val isSingleEmoji = isSingleEmoji(message.text ?: "")
            val isFirstInGroup = message.senderId != lastSenderId
            
            val actions = createDropdownActions(message, isMine, chatType)
            val updatedMessage = processAttachments(message)
            
            chatItems.add(
                ChatItem.MessageItem(
                    message = updatedMessage,
                    time = updatedMessage.sendTime.toInstance().toPrettyTime(),
                    isMine = isMine,
                    isRead = if (isMine) updatedMessage.isRead else null,
                    senderName = if (!isMine && chatType == ChatType.GROUP) {
                        userNamesCache[updatedMessage.senderId].also {
                            if (it == null) onLoadUserName(updatedMessage.senderId)
                        }
                    } else null,
                    isFirstInGroup = isFirstInGroup,
                    isSingleEmoji = isSingleEmoji,
                    dropdownActions = actions,
                    chatType = chatType,
                    readInfo = if (isMine) mergeReadInfo(updatedMessage) else null
                )
            )
            
            lastSenderId = message.senderId
        }
        return chatItems
    }
    
    private fun createDropdownActions(
        message: Message,
        isMine: Boolean,
        chatType: ChatType
    ): List<DropdownMenuAction> {
        val actions = mutableListOf<DropdownMenuAction>()
        
        if (!message.text.isNullOrBlank()) {
            actions.add(
                DropdownMenuAction(
                    Icons.Rounded.ContentCopy,
                    UiText.StringResource(R.string.copy),
                    onClick = { onCopyText(message.text) })
            )
        }
        
        if (isMine) {
            when (message.status) {
                MessageStatus.SENDING -> {
                    actions.add(
                        DropdownMenuAction(
                            Icons.Rounded.DeleteOutline,
                            UiText.StringResource(R.string.cancel_sending),
                            onClick = { onCancelSendMessage(message) },
                            isDestructive = true
                        )
                    )
                    return actions
                }
                
                MessageStatus.ERROR -> {
                    actions.add(
                        DropdownMenuAction(
                            Icons.Rounded.Refresh,
                            UiText.StringResource(R.string.retry),
                            onClick = { onRetrySendMessage(message) }
                        )
                    )
                    actions.add(
                        DropdownMenuAction(
                            Icons.Rounded.DeleteOutline,
                            UiText.StringResource(R.string.delete),
                            onClick = { onDeleteMessage(message) },
                            isDestructive = true
                        )
                    )
                    return actions
                }
                
                MessageStatus.SENT -> {}
            }
        }
        
        val isMyMessage = if (chatType == ChatType.CHANNEL) isOwner else isMine
        val now = System.currentTimeMillis()
        val twentyFourHoursMs = 24 * 60 * 60 * 1000L
        val canEdit = isMyMessage &&
                !message.text.isNullOrBlank() &&
                (now - message.sendTime) <= twentyFourHoursMs
        
        if (canEdit) {
            actions.add(
                DropdownMenuAction(
                    Icons.Outlined.Edit,
                    UiText.StringResource(R.string.edit),
                    onClick = { onEditMessage(message) }
                )
            )
        }
        
        val canDelete = when (chatType) {
            ChatType.PRIVATE -> true
            ChatType.CHANNEL -> isOwner
            ChatType.GROUP -> isOwner || isMyMessage
            else -> false
        }
        
        if (canDelete) {
            actions.add(
                DropdownMenuAction(
                    Icons.Rounded.DeleteOutline,
                    UiText.StringResource(R.string.delete),
                    onClick = { onDeleteMessage(message) },
                    isDestructive = true
                )
            )
        }
        return actions
    }
    
    private fun processAttachments(message: Message): Message {
        val updatedAttachments = message.attachments.map { attachment ->
            if (attachment.localUri != null) {
                val mimeType = attachment.localUri.getFileType(context)
                val newType = when {
                    mimeType == "image/gif" -> AttachmentType.GIF
                    mimeType.startsWith("image/") -> AttachmentType.IMAGE
                    mimeType.startsWith("video/") -> AttachmentType.VIDEO
                    mimeType.startsWith("audio/") -> AttachmentType.VOICE
                    else -> attachment.type
                }
                attachment.copy(type = newType)
            } else {
                attachment
            }
        }
        return message.copy(attachments = updatedAttachments)
    }
    
    private fun mergeReadInfo(message: Message): List<MessageReadInfo>? {
        val serverReadInfo = message.readInfo.orEmpty()
        val extraReadInfo = groupReadInfo[message.id].orEmpty()
        val merged = (serverReadInfo + extraReadInfo).distinctBy { it.userId }
        val resolved = merged.map { info ->
            if (info.firstName.isBlank()) {
                val name = userNamesCache[info.userId]
                if (name != null) {
                    val parts = name.split(" ", limit = 2)
                    info.copy(
                        firstName = parts.getOrElse(0) { "" },
                        lastName = parts.getOrNull(1)
                    )
                } else {
                    onLoadUserName(info.userId)
                    info
                }
            } else info
        }
        return resolved.ifEmpty { null }
    }
    
    private fun isSingleEmoji(text: String): Boolean {
        val emojiRegex =
            Regex("^[\\p{So}\\p{Cntrl}\\p{InEmoticons}\\p{InMiscellaneousSymbolsAndPictographs}\\p{InSupplementalSymbolsAndPictographs}\\uD83C\\uDFF0-\\uD83D\\uDFFF]+$")
        return emojiRegex.matches(text.trim())
    }
}
