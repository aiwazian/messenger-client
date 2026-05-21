/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.DoneAll
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.enums.AppPrimaryColor
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.SystemMessageEventType
import com.aiwazian.messenger.extensions.sharedBounds
import com.aiwazian.messenger.extensions.sharedElement
import com.aiwazian.messenger.extensions.toInstance
import com.aiwazian.messenger.extensions.toPrettyTime
import com.aiwazian.messenger.utils.UiText

@Composable
fun ChatCard(
    chat: Chat,
    isSelected: Boolean = false,
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
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.sharedElement(key = "chat-name-${chat.id}")
            )
        },
        supportingContent = {
            if (chat.lastMessage != null) {
                var color = Color.Unspecified
                val text = if (chat.lastMessage.attachments.isNotEmpty()) {
                    color = MaterialTheme.colorScheme.primary
                    when (chat.lastMessage.attachments.first().type) {
                        AttachmentType.IMAGE -> {
                            UiText.StringResource(R.string.photo)
                        }
                        
                        AttachmentType.FILE -> {
                            UiText.StringResource(R.string.file)
                        }
                        
                        AttachmentType.VIDEO -> {
                            UiText.StringResource(R.string.video)
                        }
                        
                        AttachmentType.VOICE -> {
                            UiText.StringResource(R.string.voice)
                        }
                    }
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
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        leadingContent = {
            Box {
                Box(Modifier.padding(4.dp)) {
                    ChatAvatar(
                        id = chat.id,
                        chatName = chat.chatName.asString(),
                        avatarUri = chat.avatarUri,
                        size = 50.dp
                    )
                }
                AnimatedVisibility(
                    visible = isSelected,
                    modifier = Modifier.align(Alignment.BottomEnd),
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                    )
                }
            }
        },
        trailingContent = {
            Column(verticalArrangement = Arrangement.Top, horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (chat.lastMessage != null) {
                        LastMessageSendTime(chat.lastMessage)
                    }
                    AnimatedVisibility(chat.isPinned) {
                        PinIcon()
                    }
                }
                
                if (unreadMessageCount > 0) {
                    UnreadMessageCount(unreadMessageCount)
                }
            }
        })
}

@Composable
private fun LastMessageSendTime(lastMessage: Message) {
    val isRead = lastMessage.isRead
    
    val sendTime = lastMessage.sendTime.toInstance().toPrettyTime()
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = if (isRead) Icons.Rounded.DoneAll else Icons.Rounded.Done,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp)
        )
        Text(text = sendTime, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun PinIcon() {
    Icon(
        imageVector = Icons.Outlined.PushPin,
        contentDescription = null,
        modifier = Modifier
            .rotate(45f)
            .size(12.dp),
    )
}

@Composable
private fun UnreadMessageCount(count: Int) {
    Badge(containerColor = MaterialTheme.colorScheme.primary) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun ChatAvatar(id: Long, chatName: String, avatarUri: Uri? = null, size: Dp = 40.dp) {
    if (avatarUri != null) {
        AsyncImage(
            model = avatarUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .sharedElement(key = "chat-avatar-$id")
                .size(size)
                .clip(CircleShape)
        )
    } else {
        Box(
            modifier = Modifier
                .sharedElement(key = "chat-avatar-$id")
                .size(size)
                .clip(CircleShape)
                .background(AppPrimaryColor.entries[(id.toString().first().code % 5) + 1].color),
            contentAlignment = Alignment.Center
        ) {
            chatName.firstOrNull()?.let { letter ->
                Text(
                    text = letter.uppercase(),
                    fontSize = 20.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.sharedElement(key = "chat-avatar-letter-$id")
                )
            }
        }
    }
}
