package com.aiwazian.messenger.ui.screens.chat

import android.content.Context
import android.webkit.MimeTypeMap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Forward
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.outlined.PushPin
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
import com.aiwazian.messenger.extensions.isVoiceRecordingExtension
import com.aiwazian.messenger.extensions.toInstance
import com.aiwazian.messenger.extensions.toPrettyTime
import com.aiwazian.messenger.ui.components.topBar.DropdownMenuAction
import com.aiwazian.messenger.utils.RegexPatterns
import com.aiwazian.messenger.utils.UiText
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

class ChatItemMapper(
    private val context: Context,
    private val myId: Long,
    private val chatId: Long,
    private val isOwner: Boolean,
    private val isJoined: Boolean,
    private val userNamesCache: Map<Long, String>,
    private val memberTagsCache: Map<Long, String> = emptyMap(),
    private val groupReadInfo: Map<Long, List<MessageReadInfo>>,
    private val highlightedMessageId: Long? = null,
    private val unreadAnchorMessageId: Long? = null,
    private val copyPolicy: ChatCopyPolicy = ChatCopyPolicy.Unrestricted,
    private val pinnedByMeMessageIds: Set<Long> = emptySet(),
    private val sharedPinnedMessageIds: Set<Long> = emptySet(),
    private val canPinForEveryone: Boolean = false,
    private val onCopyText: (Message) -> Unit,
    private val onEditMessage: (Message) -> Unit,
    private val onDeleteMessage: (Message) -> Unit,
    private val onRetrySendMessage: (Message) -> Unit,
    private val onCancelSendMessage: (Message) -> Unit,
    private val onReplyMessage: (Message) -> Unit,
    private val onForwardMessage: (Message) -> Unit,
    private val onPinMessage: (Message) -> Unit,
    private val onUnpinMessage: (Message) -> Unit,
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
            
            if (unreadAnchorMessageId != null && message.id == unreadAnchorMessageId) {
                chatItems.add(ChatItem.UnreadSeparator)
            }
            
            val isMine = message.senderId == myId && chatType != ChatType.CHANNEL
            val isSingleEmoji = isSingleEmoji(message.text ?: "")
            val isFirstInGroup = message.senderId != lastSenderId
            
            val actions = createDropdownActions(message, isMine, chatType) +
                    listOfNotNull(createNoCopyNotice(chatType))
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
    
    private fun createNoCopyNotice(chatType: ChatType): DropdownMenuAction? {
        if (!copyPolicy.hasNotice()) return null
        
        val textResId = when (chatType) {
            ChatType.CHANNEL -> R.string.no_copy_channel_notice
            ChatType.GROUP -> R.string.no_copy_group_notice
            ChatType.PRIVATE -> R.string.no_copy_private_notice
            else -> return null
        }
        
        return DropdownMenuAction(
            icon = Icons.Rounded.Block,
            text = UiText.StringResource(textResId),
            onClick = null,
            isNotice = true
        )
    }
    
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
    
    private fun createPinAction(
        message: Message,
        isSent: Boolean,
        chatType: ChatType
    ): DropdownMenuAction? {
        if (!isSent) return null
        
        val canPin = when (chatType) {
            ChatType.PRIVATE -> true
            ChatType.GROUP, ChatType.CHANNEL -> isJoined
            else -> false
        }
        if (!canPin) return null
        
        val hasSelfPin = message.id in pinnedByMeMessageIds
        val hasSharedPin = message.id in sharedPinnedMessageIds
        val canManagePin = chatType == ChatType.PRIVATE || canPinForEveryone
        
        return when {
            isSavedMessages -> when {
                hasSelfPin || hasSharedPin -> unpinAction(message)
                else -> pinAction(message)
            }
            
            canManagePin -> when {
                hasSharedPin || hasSelfPin -> editPinAction(message)
                else -> pinAction(message)
            }
            
            hasSelfPin -> unpinAction(message)
            else -> pinAction(message)
        }
    }
    
    private fun pinAction(message: Message) = DropdownMenuAction(
        Icons.Outlined.PushPin,
        UiText.StringResource(R.string.pin_message),
        onClick = { onPinMessage(message) }
    )
    
    private fun editPinAction(message: Message) = DropdownMenuAction(
        Icons.Outlined.PushPin,
        UiText.StringResource(R.string.edit_pin),
        onClick = { onPinMessage(message) }
    )
    
    private fun unpinAction(message: Message) = DropdownMenuAction(
        Icons.Outlined.PushPin,
        UiText.StringResource(R.string.unpin_message),
        onClick = { onUnpinMessage(message) }
    )
    
    private val isSavedMessages: Boolean
        get() = ChatType.fromId(chatId) == ChatType.PRIVATE && chatId == myId
    
    private fun createDropdownActions(
        message: Message,
        isMine: Boolean,
        chatType: ChatType
    ): List<DropdownMenuAction> {
        val actions = mutableListOf<DropdownMenuAction>()
        
        if (copyPolicy.canCopyText() && !message.text.isNullOrBlank()) {
            actions.add(
                DropdownMenuAction(
                    Icons.Rounded.ContentCopy,
                    UiText.StringResource(R.string.copy),
                    onClick = { onCopyText(message) })
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
        
        if (isSent && copyPolicy.canForward()) {
            actions.add(
                DropdownMenuAction(
                    Icons.AutoMirrored.Outlined.Forward,
                    UiText.StringResource(R.string.forward),
                    onClick = { onForwardMessage(message) })
            )
        }
        
        createPinAction(message, isSent, chatType)?.let { actions.add(it) }
        
        val now = System.currentTimeMillis()
        val twentyFourHoursMs = 24 * 60 * 60 * 1000L
        val isInEditWindow = isSavedMessages || (now - message.sendTime) <= twentyFourHoursMs
        
        val canEdit = isMyMessage &&
                message.forwardedFrom == null &&
                !message.text.isNullOrBlank() &&
                isInEditWindow
        
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
        var hasChanges = false
        
        val updatedAttachments = message.attachments.map { attachment ->
            val localUri = attachment.localUri ?: return@map attachment
            
            val mimeType = mimeTypeOf(attachment.extension) ?: localUri.getFileType(context)
            
            val newType = when {
                mimeType == "image/gif" -> AttachmentType.GIF
                mimeType.startsWith("image/") -> AttachmentType.IMAGE
                mimeType.startsWith("video/") -> AttachmentType.VIDEO
                mimeType.startsWith("audio/") && attachment.extension.isVoiceRecordingExtension() -> AttachmentType.VOICE
                mimeType.startsWith("audio/") -> AttachmentType.FILE
                else -> attachment.type
            }
            
            if (newType == attachment.type) {
                return@map attachment
            }
            
            hasChanges = true
            attachment.copy(type = newType)
        }
        
        return if (hasChanges) message.copy(attachments = updatedAttachments) else message
    }
    
    private fun mimeTypeOf(extension: String): String? {
        if (extension.isBlank()) {
            return null
        }
        
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
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
        return RegexPatterns.SINGLE_EMOJI.matches(text.trim())
    }
}
