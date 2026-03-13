package com.aiwazian.messenger.ui.element

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
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PushPin
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
import com.aiwazian.messenger.data.ChatInfo
import com.aiwazian.messenger.data.Message
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ChatCard(
    chatInfo: ChatInfo,
    selected: Boolean = false,
    pinned: Boolean = false,
    unreadMessageCount: Int = 0,
    onClickChat: () -> Unit = {},
    onLongClickChat: () -> Unit = {},
    onLongClickChatLogo: () -> Unit = {}
) {
    ListItem(
        modifier = Modifier.combinedClickable(
            onClick = {
                onClickChat()
            },
            onLongClick = {
                onLongClickChat()
            }),
        headlineContent = {
            Text(
                text = chatInfo.chatName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            if (chatInfo.lastMessage != null && !chatInfo.lastMessage?.text.isNullOrBlank()) {
                Text(
                    text = chatInfo.lastMessage?.text ?: chatInfo.lastMessage?.attachments[0]?.name
                    ?: "",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        leadingContent = {
            Leading(selected)
        },
        trailingContent = {
            Column {
                if (chatInfo.lastMessage != null) {
                    LastMessageSendTime(chatInfo.lastMessage!!)
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
    
    val sendTime = lastMessage.sendTime
    val sendMessageTime = formatTimestamp(sendTime)
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(contentAlignment = Alignment.CenterEnd) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            
            if (isRead) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(16.dp)
                )
            }
        }
        
        Text(sendMessageTime)
    }
}

@Composable
private fun PinIcon() {
    Badge(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Icon(
            imageVector = Icons.Outlined.PushPin,
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
            imageVector = Icons.Outlined.AccountCircle,
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
                            imageVector = Icons.Outlined.Check,
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

private fun formatTimestamp(timestamp: Long): String {
    val currentDateTime = LocalDateTime.now()
    val providedDateTime =
        Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime()
    
    if (providedDateTime.toLocalDate().isEqual(currentDateTime.toLocalDate())) {
        return providedDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    }
    
    val startOfWeek =
        currentDateTime.toLocalDate().minusDays(currentDateTime.dayOfWeek.ordinal.toLong())
    if (providedDateTime.toLocalDate().isAfter(startOfWeek) && providedDateTime.toLocalDate()
            .isBefore(currentDateTime.toLocalDate())
    ) {
        val dayOfWeekFormatter = DateTimeFormatter.ofPattern("E")
        return providedDateTime.format(dayOfWeekFormatter)
    }
    
    val dateFormatter = DateTimeFormatter.ofPattern("d MMM")
    return providedDateTime.format(dateFormatter)
}
