/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Check
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.extensions.toInstance
import com.aiwazian.messenger.extensions.toPrettyTime
import com.aiwazian.messenger.utils.SYSTEM_USER_ID

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
        modifier = Modifier.combinedClickable(
            onClick = onClickChat,
            onLongClick = onLongClickChat
        ),
        headlineContent = {
            Text(
                text = chat.chatName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            if (chat.lastMessage != null) {
                if (chat.lastMessage.files.isNotEmpty()) {
                    Text(text = chat.lastMessage.files.first().name)
                } else if (!chat.lastMessage.text.isNullOrBlank()) {
                    val isSystem = chat.lastMessage.senderId == SYSTEM_USER_ID
                    Text(
                        text = chat.lastMessage.text,
                        color = if (isSystem) MaterialTheme.colorScheme.primary else Color.Unspecified,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        leadingContent = {
            Leading(selected)
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
private fun Leading(visible: Boolean) {
    Box(modifier = Modifier.size(40.dp)) {
        Icon(
            imageVector = Icons.Rounded.AccountCircle,
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
        
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .size(16.dp)
                .align(Alignment.BottomEnd)
        ) {
            AnimatedContent(targetState = visible) { isVisible ->
                if (isVisible) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .size(16.dp)
                            .background(Color.Green),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
