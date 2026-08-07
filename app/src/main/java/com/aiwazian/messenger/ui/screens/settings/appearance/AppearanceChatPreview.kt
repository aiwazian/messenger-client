/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.appearance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.domain.MessageReplyPreview
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.enums.MessageType
import com.aiwazian.messenger.extensions.toInstance
import com.aiwazian.messenger.extensions.toPrettyTime
import com.aiwazian.messenger.ui.screens.chat.ChatItem
import com.aiwazian.messenger.ui.screens.chat.components.MessageBubble

private const val PREVIEW_CHAT_ID = 1_000_000_001L
private const val PREVIEW_PEER_ID = 1_000_000_002L
private const val PREVIEW_MY_ID = 1_000_000_003L

@Composable
fun AppearanceChatPreview(modifier: Modifier = Modifier) {
    val sendTime = remember { System.currentTimeMillis() }
    val time = remember(sendTime) { sendTime.toInstance().toPrettyTime() }
    
    val incomingText = stringResource(R.string.appearance_preview_incoming_text)
    
    val incomingMessage = ChatItem.MessageItem(
        message = Message(
            id = 1L,
            senderId = PREVIEW_PEER_ID,
            chatId = PREVIEW_CHAT_ID,
            text = incomingText,
            sendTime = sendTime,
            isRead = true,
            messageType = MessageType.TEXT,
            systemMessageEventType = null,
            attachments = emptyList()
        ),
        time = time,
        isMine = false,
        isRead = null,
        senderName = null,
        isFirstInGroup = true,
        isSingleEmoji = false,
        dropdownActions = emptyList(),
        chatType = ChatType.PRIVATE
    )
    
    val replyMessage = ChatItem.MessageItem(
        message = Message(
            id = 2L,
            senderId = PREVIEW_MY_ID,
            chatId = PREVIEW_CHAT_ID,
            text = stringResource(R.string.appearance_preview_reply_text),
            sendTime = sendTime,
            isRead = true,
            messageType = MessageType.TEXT,
            systemMessageEventType = null,
            attachments = emptyList(),
            replyTo = MessageReplyPreview(
                messageId = 1L,
                chatId = PREVIEW_CHAT_ID,
                senderId = PREVIEW_PEER_ID,
                senderName = "Karen",
                text = incomingText
            )
        ),
        time = time,
        isMine = true,
        isRead = true,
        senderName = null,
        isFirstInGroup = true,
        isSingleEmoji = false,
        dropdownActions = emptyList(),
        chatType = ChatType.PRIVATE
    )
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        MessageBubble(
            item = incomingMessage,
            onFileAction = { _, _ -> },
            showContextMenu = false
        )
        
        MessageBubble(
            item = replyMessage,
            onFileAction = { _, _ -> },
            showContextMenu = false
        )
    }
}
