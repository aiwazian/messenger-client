/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Forward
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
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
    /** Состою ли в группе/канале: без этого нельзя ни писать, ни отвечать. */
    private val isJoined: Boolean,
    private val userNamesCache: Map<Long, String>,
    /**
     * Теги участников группы: id → тег.
     *
     * Загружаются одним запросом при открытии группы, в каналах всегда пусты.
     */
    private val memberTagsCache: Map<Long, String> = emptyMap(),
    private val groupReadInfo: Map<Long, List<MessageReadInfo>>,
    private val highlightedMessageId: Long? = null,
    private val unreadAnchorMessageId: Long? = null,
    /**
     * Правила защиты контента: при запрете копирования в меню сообщения нет ни
     * «Копировать», ни «Переслать» — даже у владельца.
     */
    private val copyPolicy: ChatCopyPolicy = ChatCopyPolicy.Unrestricted,
    private val onCopyText: (String) -> Unit,
    private val onEditMessage: (Message) -> Unit,
    private val onDeleteMessage: (Message) -> Unit,
    private val onRetrySendMessage: (Message) -> Unit,
    private val onCancelSendMessage: (Message) -> Unit,
    private val onReplyMessage: (Message) -> Unit,
    private val onForwardMessage: (Message) -> Unit,
    private val onLoadUserName: (Long) -> Unit
) {
    fun map(messages: List<Message>): List<ChatItem> {
        val chatItems = mutableListOf<ChatItem>()
        var lastDate: java.time.LocalDate? = null
        var lastSenderId: Long? = null
        
        val chatType = ChatType.fromId(chatId)
        
        /*
         * При запрете копирования пункты меню не просто исчезают: внизу меню
         * остаётся пояснение, почему нет «Копировать» и «Переслать».
         */
        val noCopyNotice = if (copyPolicy.noCopy) {
            when (chatType) {
                ChatType.CHANNEL -> DropdownMenuAction(
                    icon = Icons.Rounded.Block,
                    text = UiText.StringResource(R.string.no_copy_channel_notice),
                    onClick = null,
                    isNotice = true
                )
                
                ChatType.GROUP -> DropdownMenuAction(
                    icon = Icons.Rounded.Block,
                    text = UiText.StringResource(R.string.no_copy_group_notice),
                    onClick = null,
                    isNotice = true
                )
                
                else -> null
            }
        } else null
        
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
            
            if (unreadAnchorMessageId != null && message.id == unreadAnchorMessageId) {
                chatItems.add(ChatItem.UnreadSeparator)
            }
            
            val isMine = message.senderId == myId && chatType != ChatType.CHANNEL
            val isSingleEmoji = isSingleEmoji(message.text ?: "")
            val isFirstInGroup = message.senderId != lastSenderId
            
            val actions =
                createDropdownActions(message, isMine, chatType) + listOfNotNull(noCopyNotice)
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
                    /* Тег есть только в группах и только у назначенных администраторов. */
                    senderTag = if (!isMine && chatType == ChatType.GROUP) {
                        memberTagsCache[updatedMessage.senderId]?.takeIf { it.isNotBlank() }
                    } else null,
                    isFirstInGroup = isFirstInGroup,
                    isSingleEmoji = isSingleEmoji,
                    dropdownActions = actions,
                    chatType = chatType,
                    isHighlighted = highlightedMessageId != null && updatedMessage.id == highlightedMessageId,
                    readInfo = if (isMine) mergeReadInfo(updatedMessage) else null,
                    canReply = canReply(updatedMessage, chatType)
                )
            )
            
            lastSenderId = message.senderId
        }
        return chatItems
    }
    
    /**
     * Ответить можно только там, где вообще разрешено писать: в канале —
     * только владельцу, в группе — только участнику, в личном чате — всегда.
     *
     * Тем же условием включается свайп влево в MessageBubble.
     */
    private fun canReply(message: Message, chatType: ChatType): Boolean {
        if (message.messageType == MessageType.SYSTEM) return false
        if (message.id <= 0 || message.status != MessageStatus.SENT) return false
        return when (chatType) {
            ChatType.PRIVATE -> true
            ChatType.GROUP -> isJoined
            ChatType.CHANNEL -> isOwner
            else -> false
        }
    }
    
    private fun createDropdownActions(
        message: Message,
        isMine: Boolean,
        chatType: ChatType
    ): List<DropdownMenuAction> {
        val actions = mutableListOf<DropdownMenuAction>()
        
        if (copyPolicy.canCopyText && !message.text.isNullOrBlank()) {
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
        
        val isSent = message.id > 0 && message.status == MessageStatus.SENT
        
        if (canReply(message, chatType)) {
            actions.add(
                DropdownMenuAction(
                    Icons.AutoMirrored.Outlined.Reply,
                    UiText.StringResource(R.string.reply),
                    onClick = { onReplyMessage(message) })
            )
        }
        
        if (isSent && copyPolicy.canForward) {
            actions.add(
                DropdownMenuAction(
                    Icons.AutoMirrored.Outlined.Forward,
                    UiText.StringResource(R.string.forward),
                    onClick = { onForwardMessage(message) })
            )
        }
        
        val now = System.currentTimeMillis()
        val twentyFourHoursMs = 24 * 60 * 60 * 1000L
        
        /**
         * Пересланное сообщение — копия чужого текста, редактировать его нельзя.
         * Сервер проверяет это же условие в CanEditMessageGuard.
         */
        val canEdit = isMyMessage &&
                message.forwardedFrom == null &&
                !message.text.isNullOrBlank() &&
                (now - message.sendTime) <= twentyFourHoursMs
        
        if (canEdit) {
            actions.add(
                DropdownMenuAction(
                    Icons.Rounded.Edit,
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
