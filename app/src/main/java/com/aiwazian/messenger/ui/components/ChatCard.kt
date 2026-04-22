/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.enums.SystemMessageEventType
import com.aiwazian.messenger.extensions.sharedBounds
import com.aiwazian.messenger.extensions.sharedElement
import com.aiwazian.messenger.extensions.toInstance
import com.aiwazian.messenger.extensions.toPrettyTime
import com.aiwazian.messenger.utils.UiText

@Composable
fun ChatCard(
    chat: Chat,
    selected: Boolean = false,
    pinned: Boolean = false,
    unreadMessageCount: Int = 0,
    onClickChat: () -> Unit = {},
    onLongClickChat: () -> Unit = {},
    onLongClickChatLogo: () -> Unit = {}
) {
    ListItem(
        modifier = Modifier
            .combinedClickable(
                onClick = onClickChat,
                onLongClick = onLongClickChat
            )
            .sharedBounds(key = "chat-${chat.id}"),
        headlineContent = {
            Text(
                text = chat.chatName.asString(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.sharedElement(key = "chat-name-${chat.id}")
            )
        },
        supportingContent = {
            if (chat.lastMessage != null) {
                var color = Color.Unspecified
                val text = if (chat.lastMessage.attachments.isNotEmpty()) {
                    UiText.DynamicString(chat.lastMessage.attachments.first().name)
                } else if (!chat.lastMessage.text.isNullOrBlank()) {
                    UiText.DynamicString(chat.lastMessage.text)
                } else if (chat.lastMessage.systemMessageEventType != null) {
                    color = MaterialTheme.colorScheme.primary
                    UiText.StringResource(
                        when (chat.lastMessage.systemMessageEventType) {
                            SystemMessageEventType.CHANNEL_CREATED -> R.string.channel_created
                            SystemMessageEventType.GROUP_CREATED -> R.string.group_created
                            SystemMessageEventType.HISTORY_CLEARED -> R.string.history_cleared
                        }
                    )
                } else UiText.DynamicString("")
                
                Text(
                    text = text.asString(),
                    maxLines = 1,
                    color = color,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        leadingContent = {
            Leading(chat.id)
        },
        trailingContent = {
            Column {
                if (chat.lastMessage != null) {
                    LastMessageSendTime(chat.lastMessage)
                }
                
                Box(modifier = Modifier.size(40.dp)) {
                    if (unreadMessageCount > 0) {
                        UnreadMessageCount(unreadMessageCount)
                    } else if (pinned) {
                        PinIcon()
                    }
                }
            }
        })
}

@Composable
private fun LastMessageSendTime(lastMessage: Message) {
    val isRead = lastMessage.isRead
    
    val sendTime = lastMessage.sendTime.toInstance().toPrettyTime()
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (isRead) Icons.Rounded.DoneAll else Icons.Rounded.Done,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Text(sendTime)
    }
}

@Composable
private fun PinIcon() {
    Badge(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Icon(
            imageVector = Icons.Rounded.PushPin,
            contentDescription = null,
            modifier = Modifier.rotate(45f),
        )
    }
}

@Composable
private fun UnreadMessageCount(count: Int) {
    Badge(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = Color.White
    ) {
        Text(
            text = count.toString(),
            fontSize = 14.sp,
            modifier = Modifier.padding(2.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun Leading(id: Long) {
    Box(modifier = Modifier.size(40.dp)) {
        Icon(
            imageVector = Icons.Rounded.AccountCircle,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .sharedElement(key = "avatar-$id")
        )
    }
}
