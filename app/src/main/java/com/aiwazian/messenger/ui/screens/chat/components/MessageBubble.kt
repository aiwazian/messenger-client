/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.MessageAttachment
import com.aiwazian.messenger.domain.MessageReadInfo
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.enums.FileAction
import com.aiwazian.messenger.extensions.formatFileSize
import com.aiwazian.messenger.extensions.getDuration
import com.aiwazian.messenger.extensions.sharedElement
import com.aiwazian.messenger.extensions.toInstance
import com.aiwazian.messenger.extensions.toPrettyTime
import com.aiwazian.messenger.ui.app.AppDropdownMenu
import com.aiwazian.messenger.ui.app.AppDropdownMenuItem
import com.aiwazian.messenger.ui.components.ChatAvatar
import com.aiwazian.messenger.ui.components.chatMediaKey
import com.aiwazian.messenger.ui.components.formatDuration
import com.aiwazian.messenger.ui.components.mediaTransitionOrigin
import com.aiwazian.messenger.ui.components.topBar.DropdownMenuAction
import com.aiwazian.messenger.ui.screens.chat.ChatItem
import com.aiwazian.messenger.utils.UiText
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Отступ от края ленты до пузыря. Он же съедает ширину у содержимого пузыря. */
private val BubbleHorizontalPadding = 8.dp

@Composable
fun MessageBubble(
    modifier: Modifier = Modifier,
    item: ChatItem.MessageItem,
    onFileAction: (MessageAttachment, FileAction) -> Unit,
    currentPlayingVoiceFileId: String? = null,
    isVoicePlaying: Boolean = false,
    voicePositionMs: Int = 0,
    voiceDurationMs: Int = 0,
    onVoiceSeek: (MessageAttachment, Int) -> Unit = { _, _ -> },
    onLinkClicked: ((String) -> Unit)? = null,
    onUsernameClicked: ((String) -> Unit)? = null,
    onEmailClicked: ((String) -> Unit)? = null,
    onSaveToDownloads: (() -> Unit)? = null,
    onReplyPreviewClick: (() -> Unit)? = null,
    onForwardedFromClick: (() -> Unit)? = null,
    onSwipeThresholdReached: (() -> Unit)? = null,
    onSwipeToReply: (() -> Unit)? = null,
    readerAvatars: Map<Long, Uri?> = emptyMap(),
    onReadersRequested: ((List<Long>) -> Unit)? = null,
    onSenderNameClick: (() -> Unit)? = null,
    onReaderClick: ((MessageReadInfo) -> Unit)? = null,
    showContextMenu: Boolean = true
) {
    val message = item.message
    var expanded by remember { mutableStateOf(false) }
    var showReadersDropdown by remember { mutableStateOf(false) }
    val alignment = if (item.isMine) Alignment.CenterEnd else Alignment.CenterStart
    val isSavedMessages =
        item.chatType == ChatType.PRIVATE && item.message.senderId == item.message.chatId
    
    val backgroundColor by animateColorAsState(
        targetValue = if (item.isHighlighted) MaterialTheme.colorScheme.primary.copy(
            alpha = 0.1f
        ) else Color.Transparent
    )
    
    SwipeToReplyBox(
        enabled = item.canReply && onSwipeToReply != null,
        onReply = { onSwipeToReply?.invoke() },
        onThresholdReached = { onSwipeThresholdReached?.invoke() },
        modifier = modifier.background(backgroundColor)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { if (showContextMenu) expanded = true },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }),
            contentAlignment = alignment
        ) {
            val dynamicMaxWidth = when {
                maxWidth < 360.dp -> 280.dp
                
                maxWidth <= 411.dp -> 310.dp
                
                maxWidth < 600.dp -> 360.dp
                
                maxWidth < 840.dp -> 440.dp
                
                else -> 520.dp
            }
            
            val contentMaxWidth = dynamicMaxWidth - BubbleHorizontalPadding * 2
            
            val containerColor =
                if (item.isMine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
            
            Box(
                modifier = Modifier
                    .widthIn(min = 90.dp, max = dynamicMaxWidth)
                    .padding(horizontal = BubbleHorizontalPadding)
                    .clip(MaterialTheme.shapes.large)
                    .background(containerColor)
            ) {
                Column(Modifier.width(IntrinsicSize.Max)) {
                    if (!item.isMine && item.isFirstInGroup && item.senderName != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, top = 8.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = if (onSenderNameClick != null) {
                                    Modifier
                                        .clip(MaterialTheme.shapes.small)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = ripple(color = MaterialTheme.colorScheme.primary),
                                            onClick = onSenderNameClick,
                                        )
                                        .padding(horizontal = 4.dp)
                                } else {
                                    Modifier
                                }
                            ) {
                                Text(
                                    text = item.senderName,
                                    fontSize = 12.sp,
                                    lineHeight = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            
                            if (!item.senderTag.isNullOrBlank()) {
                                Spacer(Modifier.width(4.dp))
                                
                                Box(
                                    modifier = Modifier
                                        .clip(MaterialTheme.shapes.medium)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                        .padding(horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = item.senderTag,
                                        fontSize = 11.sp,
                                        lineHeight = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                    
                    message.forwardedFrom?.let { forwardedFrom ->
                        ForwardedFromHeader(
                            forwardedFrom = forwardedFrom,
                            modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp),
                            onClick = { onForwardedFromClick?.invoke() })
                    }
                    
                    message.replyTo?.let { preview ->
                        ReplyQuote(
                            preview = preview,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, top = 8.dp, end = 8.dp),
                            onClick = onReplyPreviewClick
                        )
                    }
                    
                    val mediaAttachments = message.attachments.filter {
                        it.type == AttachmentType.IMAGE || it.type == AttachmentType.VIDEO || it.type == AttachmentType.GIF
                    }
                    if (mediaAttachments.isNotEmpty()) {
                        val mediaSizes = mediaAttachments.map { attachment ->
                            val frameWidth = attachment.width ?: 0
                            val frameHeight = attachment.height ?: 0
                            
                            if (frameWidth > 0 && frameHeight > 0) IntSize(frameWidth, frameHeight)
                            else IntSize.Zero
                        }
                        
                        ImageGridCustomLayout(
                            maxWidth = contentMaxWidth,
                            itemSizes = mediaSizes,
                            content = {
                                mediaAttachments.forEach { attachment ->
                                    if (attachment.localUri == null ||
                                        attachment.status == DownloadStatus.UPLOADING ||
                                        attachment.status == DownloadStatus.DOWNLOADING
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(MaterialTheme.shapes.extraSmall)
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(
                                                        alpha = 0.4f
                                                    )
                                                )
                                                .clickable {
                                                    val action = when (attachment.status) {
                                                        DownloadStatus.DOWNLOADING -> FileAction.DOWNLOAD
                                                        DownloadStatus.PAUSED -> FileAction.DOWNLOAD
                                                        DownloadStatus.IDLE,
                                                        DownloadStatus.CANCELLED,
                                                        DownloadStatus.FAILED,
                                                        DownloadStatus.UPLOADED,
                                                        DownloadStatus.COMPLETED -> FileAction.DOWNLOAD
                                                        
                                                        DownloadStatus.UPLOADING -> FileAction.CANCEL
                                                    }
                                                    onFileAction(attachment, action)
                                                }, contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = attachment.size.formatFileSize(),
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(4.dp),
                                                fontSize = 12.sp,
                                                lineHeight = 12.sp
                                            )
                                            when (attachment.status) {
                                                DownloadStatus.DOWNLOADING -> {
                                                    CircularWavyProgressIndicator()
                                                    Icon(Icons.Rounded.Pause, null)
                                                }
                                                
                                                DownloadStatus.UPLOADING -> {
                                                    CircularWavyProgressIndicator()
                                                    Icon(Icons.Rounded.Close, null)
                                                }
                                                
                                                DownloadStatus.PAUSED -> {
                                                    Icon(Icons.Rounded.Downloading, null)
                                                }
                                                
                                                else -> {
                                                    Icon(Icons.Rounded.Download, null)
                                                }
                                            }
                                        }
                                    } else {
                                        if (attachment.type == AttachmentType.VIDEO) {
                                            VideoThumbnail(attachment.localUri) {
                                                onFileAction(attachment, FileAction.OPEN)
                                            }
                                        } else {
                                            ImageThumbnail(
                                                attachment.localUri,
                                                attachment.type == AttachmentType.GIF
                                            ) {
                                                onFileAction(attachment, FileAction.OPEN)
                                            }
                                        }
                                    }
                                }
                            })
                    }
                    
                    message.attachments.forEach { attachment ->
                        when (attachment.type) {
                            AttachmentType.VOICE -> {
                                MessageVoice(
                                    file = attachment,
                                    isPlaying = currentPlayingVoiceFileId == attachment.fileId && isVoicePlaying,
                                    positionMs = if (currentPlayingVoiceFileId == attachment.fileId) voicePositionMs else 0,
                                    durationMs = if (currentPlayingVoiceFileId == attachment.fileId) voiceDurationMs else 0,
                                    onAction = { action ->
                                        onFileAction(attachment, action)
                                    },
                                    onSeek = { positionMs ->
                                        onVoiceSeek(attachment, positionMs)
                                    }
                                )
                            }
                            
                            AttachmentType.FILE -> {
                                MessageFile(
                                    file = attachment, onAction = { action ->
                                        onFileAction(attachment, action)
                                    })
                            }
                            
                            else -> {}
                        }
                    }
                    
                    if (!message.text.isNullOrBlank()) {
                        MessageText(
                            text = message.text,
                            onLinkClicked = onLinkClicked,
                            onUsernameClicked = onUsernameClicked,
                            onEmailClicked = onEmailClicked
                        )
                    }
                }
                
                Box(modifier = Modifier.align(Alignment.BottomEnd)) {
                    MessageFooter(
                        time = item.time,
                        isRead = if (item.isMine && !isSavedMessages) item.isRead else null,
                        status = message.status,
                        isEdited = message.isEdited
                    )
                }
                
                val readers = remember(item.readInfo) {
                    item.readInfo.orEmpty().sortedByDescending { it.readAt }
                }
                
                MessageDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    actions = buildDropdownActions(item, isSavedMessages, onSaveToDownloads) {
                        onReadersRequested?.invoke(readers.map { it.userId })
                        showReadersDropdown = true
                    }
                )
                
                if (readers.isNotEmpty()) {
                    AppDropdownMenu(
                        expanded = showReadersDropdown,
                        onDismissRequest = { showReadersDropdown = false },
                        properties = PopupProperties(focusable = true)
                    ) {
                        AppDropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Назад"
                                )
                            },
                            text = "",
                            onClick = {
                                showReadersDropdown = false
                                expanded = true
                            }
                        )
                        
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 10.dp))
                        
                        readers.forEach { reader ->
                            val name = listOf(reader.firstName, reader.lastName.orEmpty())
                                .filter { it.isNotBlank() }
                                .joinToString(" ")
                                .ifBlank { reader.userId.toString() }
                            val readTime = formatStatusTime(reader.readAt, todayVerb = "сегодня")
                            AppDropdownMenuItem(
                                leadingIcon = {
                                    ChatAvatar(
                                        id = reader.userId,
                                        chatName = name,
                                        avatarUri = readerAvatars[reader.userId],
                                        size = 30.dp
                                    )
                                },
                                text = name,
                                supportingText = readTime,
                                onClick = {
                                    showReadersDropdown = false
                                    onReaderClick?.invoke(reader)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * «Прочитано в 14:03» сегодня, «вчера в 22:10» и «12 августа в 14:42» дальше.
 *
 * @param todayVerb слово перед временем для сегодняшнего дня: «Прочитано», «Изменено»
 * или «сегодня» в списке читателей.
 */
private fun formatStatusTime(timestamp: Long, todayVerb: String): String {
    val instant = timestamp.toInstance()
    val date = instant.atZone(ZoneId.systemDefault())
    val today = LocalDate.now()
    val time = instant.toPrettyTime()
    
    return when (date.toLocalDate()) {
        today -> "$todayVerb в $time"
        today.minusDays(1) -> "вчера в $time"
        else -> date.format(DateTimeFormatter.ofPattern("d MMMM")) + " в " + time
    }
}

private fun buildDropdownActions(
    item: ChatItem.MessageItem,
    isSavedMessages: Boolean,
    onSaveToDownloads: (() -> Unit)? = null,
    onReadCountClick: () -> Unit = {}
): List<DropdownMenuAction> {
    val actions = mutableListOf<DropdownMenuAction>()
    
    if (item.message.editedAt != null) {
        actions.add(
            DropdownMenuAction(
                icon = Icons.Rounded.EditCalendar,
                text = UiText.DynamicString(
                    formatStatusTime(item.message.editedAt, todayVerb = "Изменено")
                ),
                onClick = null
            )
        )
    }
    
    if (item.isMine && !isSavedMessages) {
        val readInfo = item.readInfo
        
        if (item.chatType == ChatType.PRIVATE) {
            val readAt = readInfo?.maxOfOrNull { it.readAt }
            
            if (readAt != null) {
                actions.add(
                    DropdownMenuAction(
                        icon = Icons.Rounded.DoneAll,
                        text = UiText.DynamicString(
                            formatStatusTime(readAt, todayVerb = "Прочитано")
                        ),
                        onClick = null
                    )
                )
            }
        } else if (item.chatType == ChatType.GROUP && !readInfo.isNullOrEmpty()) {
            val count = readInfo.size
            val word = when {
                count % 10 == 1 && count % 100 != 11 -> "просмотр"
                count % 10 in 2..4 && count % 100 !in 12..14 -> "просмотра"
                else -> "просмотров"
            }
            actions.add(
                DropdownMenuAction(
                    icon = Icons.Rounded.DoneAll,
                    text = UiText.DynamicString("$count $word"),
                    onClick = onReadCountClick
                )
            )
        }
    }
    
    val hasDownloadedAttachment = item.message.attachments.any { attachment ->
        attachment.localUri != null &&
                (attachment.status == DownloadStatus.COMPLETED || attachment.status == DownloadStatus.UPLOADED)
    }
    if (hasDownloadedAttachment && onSaveToDownloads != null) {
        actions.add(
            DropdownMenuAction(
                icon = Icons.Rounded.SaveAlt,
                text = UiText.StringResource(R.string.save_to_downloads),
                onClick = onSaveToDownloads
            )
        )
    }
    
    actions.addAll(item.dropdownActions)
    return actions
}

@Composable
private fun VideoThumbnail(videoUri: Uri, onClick: () -> Unit) {
    val context = LocalContext.current
    
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .mediaTransitionOrigin(chatMediaKey(videoUri))
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(videoUri)
                .decoderFactory(VideoFrameDecoder.Factory())
                .videoFrameMillis(0)
                .build(),
            contentDescription = "Thumbnail of the video",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .sharedElement(key = videoUri.toString())
                .fillMaxSize()
                .clip(MaterialTheme.shapes.extraSmall)
        )
        Box(
            modifier = Modifier
                .padding(start = 4.dp, top = 4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f))
        ) {
            Text(
                text = formatDuration(videoUri.getDuration(context)),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(vertical = 2.dp, horizontal = 4.dp),
                fontSize = 12.sp,
                lineHeight = 12.sp
            )
        }
    }
}

@Composable
private fun ImageThumbnail(imageUri: Uri, isGif: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .mediaTransitionOrigin(chatMediaKey(imageUri))
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUri)
                .decoderFactory(GifDecoder.Factory())
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .sharedElement(key = imageUri.toString())
                .fillMaxSize()
                .clip(MaterialTheme.shapes.extraSmall)
        )
        if (isGif) {
            Box(
                modifier = Modifier
                    .padding(start = 4.dp, top = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f))
            ) {
                Text(
                    text = stringResource(R.string.gif).uppercase(),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(vertical = 2.dp, horizontal = 4.dp),
                    fontSize = 12.sp,
                    lineHeight = 12.sp
                )
            }
        }
    }
}
