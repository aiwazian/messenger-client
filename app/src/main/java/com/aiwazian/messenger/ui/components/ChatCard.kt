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
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.enums.AppPrimaryColor
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.enums.SystemMessageEventType
import com.aiwazian.messenger.extensions.sharedBounds
import com.aiwazian.messenger.extensions.sharedElement
import com.aiwazian.messenger.extensions.toInstance
import com.aiwazian.messenger.extensions.toPrettyTime

@Composable
fun ChatCard(
    modifier: Modifier = Modifier,
    chat: Chat,
    myId: Long = 0,
    isSelected: Boolean = false,
    isOnline: Boolean = false,
    unreadMessageCount: Int = 0,
    onClickChat: () -> Unit = {},
    onLongClickChat: () -> Unit = {}
) {
    val isSavedMessages = chat.id == myId
    
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier.combinedClickable(
            onClick = onClickChat, onLongClick = onLongClickChat
        ),
        content = {
            Text(
                text = chat.chatName.asString(),
                maxLines = 1,
                fontSize = 16.sp,
                lineHeight = 16.sp,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            var text = AnnotatedString("")
            
            if (!chat.draftText.isNullOrBlank()) {
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.error)) {
                        append(stringResource(R.string.draft) + ": ")
                    }
                    append(chat.draftText.trim())
                }
            } else if (chat.lastMessage != null) {
                if (chat.lastMessage.attachments.isNotEmpty()) {
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            append(
                                stringResource(
                                    when (chat.lastMessage.attachments.first().type) {
                                        AttachmentType.IMAGE -> R.string.photo
                                        AttachmentType.FILE -> R.string.file
                                        AttachmentType.VIDEO -> R.string.video
                                        AttachmentType.VOICE -> R.string.voice_message
                                        AttachmentType.GIF -> R.string.gif
                                    }
                                )
                            )
                        }
                    }
                } else if (!chat.lastMessage.text.isNullOrBlank()) {
                    text = buildAnnotatedString {
                        val isGroupChat = ChatType.fromId(chat.id) == ChatType.GROUP
                        val isMyMessage = chat.lastMessage.senderId == myId
                        val showMyPrefix =
                            isGroupChat && isMyMessage && chat.lastMessage.systemMessageEventType == null
                        if (showMyPrefix) {
                            append(stringResource(R.string.you) + ": ")
                        }
                        append(chat.lastMessage.text.trim())
                    }
                } else if (chat.lastMessage.systemMessageEventType != null) {
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            append(
                                stringResource(
                                    when (chat.lastMessage.systemMessageEventType) {
                                        SystemMessageEventType.CHANNEL_CREATED -> R.string.channel_created
                                        SystemMessageEventType.GROUP_CREATED -> R.string.group_created
                                        SystemMessageEventType.HISTORY_CLEARED -> R.string.history_cleared
                                    }
                                )
                            )
                        }
                    }
                }
            }
            
            Text(
                text = text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall
            )
        },
        leadingContent = {
            Box {
                Box(Modifier.padding(vertical = 4.dp)) {
                    if (isSavedMessages) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.BookmarkBorder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    } else {
                        ChatAvatar(
                            id = chat.id,
                            chatName = chat.chatName.asString(),
                            avatarUri = chat.avatarUri,
                            size = 50.dp
                        )
                    }
                }
                AnimatedVisibility(
                    visible = isOnline,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 2.dp, bottom = 2.dp),
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Circle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(2.dp)
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
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
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                    )
                }
            }
        },
        trailingContent = {
            Column(
                verticalArrangement = Arrangement.Top, horizontalAlignment = Alignment.End
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (chat.lastMessage != null) {
                        LastMessageSendTime(chat.lastMessage, isSavedMessages, myId)
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
private fun LastMessageSendTime(lastMessage: Message, isSavedMessages: Boolean, myId: Long) {
    val isRead = lastMessage.isRead
    val isMyMessage = lastMessage.senderId == myId
    val showCheckmarks = !isSavedMessages && isMyMessage
    
    val sendTime = lastMessage.sendTime.toInstance().toPrettyTime()
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (showCheckmarks) {
            Icon(
                imageVector = if (isRead) Icons.Rounded.DoneAll else Icons.Rounded.Done,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
        }
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
            text = count.toString(), style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun ChatAvatar(
    id: Long,
    chatName: String,
    avatarUri: Uri? = null,
    size: Dp = 40.dp,
    sharedTransition: Boolean = false
) {
    if (avatarUri != null) {
        AsyncImage(
            model = avatarUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .then(if (sharedTransition) Modifier.sharedElement(key = "chat-avatar-$id") else Modifier)
                .size(size)
                .clip(CircleShape)
        )
    } else {
        Box(
            modifier = Modifier
                .then(if (sharedTransition) Modifier.sharedBounds(key = "chat-avatar-$id") else Modifier)
                .size(size)
                .clip(CircleShape)
                .background(AppPrimaryColor.entries[(id.toString().first().code % 5) + 1].color),
            contentAlignment = Alignment.Center
        ) {
            chatName.firstOrNull()?.let { letter ->
                val fontSize = (size.value / 2).sp
                Text(
                    text = letter.uppercase(),
                    fontSize = fontSize,
                    lineHeight = fontSize,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = if (sharedTransition) {
                        Modifier.sharedElement(key = "chat-avatar-letter-$id")
                    } else {
                        Modifier
                    }
                )
            }
        }
    }
}
