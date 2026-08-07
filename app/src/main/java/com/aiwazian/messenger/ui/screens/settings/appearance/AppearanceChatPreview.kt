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
import com.aiwazian.messenger.domain.ForwardedFrom
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.domain.MessageReadInfo
import com.aiwazian.messenger.domain.MessageReplyPreview
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.enums.ForwardSourceAccess
import com.aiwazian.messenger.enums.MessageType
import com.aiwazian.messenger.extensions.toInstance
import com.aiwazian.messenger.extensions.toPrettyTime
import com.aiwazian.messenger.ui.screens.chat.ChatItem
import com.aiwazian.messenger.ui.screens.chat.components.MessageBubble

/*
 * Идентификаторы выдуманной переписки.
 *
 * Первая цифра задаёт тип чата, поэтому чат здесь групповой: только в группе
 * у входящего сообщения видно имя отправителя.
 */
private const val PREVIEW_CHAT_ID = 3_000_000_001L
private const val PREVIEW_MY_ID = 1_000_000_001L
private const val PREVIEW_REPLY_AUTHOR_ID = 1_000_000_002L
private const val PREVIEW_FORWARD_SOURCE_ID = 1_000_000_003L

/**
 * Превью переписки на экране внешнего вида.
 *
 * Показывает, как тема и основной цвет выглядят в чате: исходящий пузырь с
 * пересылкой и входящий ответ на него. Так в кадр попадает максимум
 * оформления — имя отправителя, заголовок пересылки, цитата, галочки
 * прочтения и пометка об изменении.
 *
 * Переписки не существует: сообщения кликабельны, но открывают только
 * собственное меню и никуда не ведут.
 */
@Composable
fun AppearanceChatPreview(modifier: Modifier = Modifier) {
    // Время текущее, иначе в подписях появится вчерашняя дата.
    val sendTime = remember { System.currentTimeMillis() }
    val time = remember(sendTime) { sendTime.toInstance().toPrettyTime() }
    
    val myName = stringResource(R.string.appearance_preview_my_name)
    val replyAuthorName = stringResource(R.string.appearance_preview_reply_author)
    val forwardedText = stringResource(R.string.appearance_preview_forwarded_text)
    
    val forwardedMessage = ChatItem.MessageItem(
        message = Message(
            id = 1L,
            senderId = PREVIEW_MY_ID,
            chatId = PREVIEW_CHAT_ID,
            text = forwardedText,
            sendTime = sendTime,
            isRead = true,
            messageType = MessageType.TEXT,
            systemMessageEventType = null,
            attachments = emptyList(),
            forwardedFrom = ForwardedFrom(
                chatId = PREVIEW_FORWARD_SOURCE_ID,
                name = stringResource(R.string.appearance_preview_forward_author),
                access = ForwardSourceAccess.OPEN
            )
        ),
        time = time,
        isMine = true,
        isRead = true,
        senderName = null,
        isFirstInGroup = true,
        isSingleEmoji = false,
        dropdownActions = emptyList(),
        chatType = ChatType.GROUP,
        readInfo = listOf(
            MessageReadInfo(
                userId = PREVIEW_REPLY_AUTHOR_ID,
                firstName = replyAuthorName,
                lastName = null,
                readAt = sendTime
            )
        )
    )
    
    val replyMessage = ChatItem.MessageItem(
        message = Message(
            id = 2L,
            senderId = PREVIEW_REPLY_AUTHOR_ID,
            chatId = PREVIEW_CHAT_ID,
            text = stringResource(R.string.appearance_preview_reply_text),
            sendTime = sendTime,
            editedAt = sendTime,
            isRead = true,
            messageType = MessageType.TEXT,
            systemMessageEventType = null,
            attachments = emptyList(),
            replyTo = MessageReplyPreview(
                messageId = 1L,
                chatId = PREVIEW_CHAT_ID,
                senderId = PREVIEW_MY_ID,
                senderName = myName,
                text = forwardedText
            )
        ),
        time = time,
        isMine = false,
        isRead = null,
        senderName = replyAuthorName,
        isFirstInGroup = true,
        isSingleEmoji = false,
        dropdownActions = emptyList(),
        chatType = ChatType.GROUP
    )
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        MessageBubble(
            item = forwardedMessage,
            onFileAction = { _, _ -> },
            onForwardedFromClick = { })
        
        MessageBubble(
            item = replyMessage,
            onFileAction = { _, _ -> },
            onReplyPreviewClick = { })
    }
}
