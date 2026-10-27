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
import androidx.compose.foundation.layout.ExperimentalFlexBoxApi
import androidx.compose.foundation.layout.FlexAlignSelf
import androidx.compose.foundation.layout.FlexBox
import androidx.compose.foundation.layout.FlexDirection
import androidx.compose.foundation.layout.FlexJustifyContent
import androidx.compose.foundation.layout.FlexWrap
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.ChatBubble
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.gif.GifDecoder
import coil3.request.ImageRequest
import coil3.video.VideoFrameDecoder
import coil3.video.videoFrameMillis
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.AudioTrackMetadata
import com.aiwazian.messenger.domain.MessageAttachment
import com.aiwazian.messenger.domain.MessageReadInfo
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.enums.FileAction
import com.aiwazian.messenger.enums.MessageType
import com.aiwazian.messenger.extensions.formatFileSize
import com.aiwazian.messenger.extensions.getDuration
import com.aiwazian.messenger.extensions.isAudioFile
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
import com.aiwazian.messenger.ui.screens.chat.ChatEmojiViewModel
import com.aiwazian.messenger.ui.screens.chat.ChatItem
import com.aiwazian.messenger.ui.screens.chat.ChatStickersViewModel
import com.aiwazian.messenger.utils.EmojiLink
import com.aiwazian.messenger.utils.StickerLink
import com.aiwazian.messenger.utils.UiText
import com.aiwazian.messenger.utils.compactCount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val BubbleHorizontalPadding = 8.dp

private const val MEDIA_CACHE_KEY_PREFIX = "chat-media"

@OptIn(ExperimentalFlexBoxApi::class)
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
    audioMetadata: Map<String, AudioTrackMetadata> = emptyMap(),
    currentMusicFileId: String? = null,
    isMusicPlaying: Boolean = false,
    musicPositionMs: Int = 0,
    musicDurationMs: Int = 0,
    onMusicSeek: (MessageAttachment, Int) -> Unit = { _, _ -> },
    onLinkClicked: ((String) -> Unit)? = null,
    onUsernameClicked: ((String) -> Unit)? = null,
    onEmailClicked: ((String) -> Unit)? = null,
    onPhoneClicked: ((String) -> Unit)? = null,
    onSaveToDownloads: (() -> Unit)? = null,
    onReplyPreviewClick: (() -> Unit)? = null,
    onForwardedFromClick: (() -> Unit)? = null,
    onContactClick: (() -> Unit)? = null,
    onSwipeThresholdReached: (() -> Unit)? = null,
    onSwipeToReply: (() -> Unit)? = null,
    readerAvatars: Map<Long, Uri?> = emptyMap(),
    onReadersRequested: ((List<Long>) -> Unit)? = null,
    onSenderNameClick: (() -> Unit)? = null,
    onReaderClick: ((MessageReadInfo) -> Unit)? = null,
    showContextMenu: Boolean = true,
    isPinned: Boolean = false,
    mediaKeyScope: String = "chat",
    onCommentsClick: (() -> Unit)? = null
) {
    val message = item.message
    var expanded by remember { mutableStateOf(false) }
    var showReadersDropdown by remember { mutableStateOf(false) }
    val alignment = if (item.isMine) Alignment.CenterEnd else Alignment.CenterStart
    val isSavedMessages =
        item.chatType == ChatType.PRIVATE && item.message.senderId == item.message.chatId
    
    val stickersViewModel: ChatStickersViewModel = hiltViewModel()
    val stickersState by stickersViewModel.uiState.collectAsState()
    
    val emojiViewModel: ChatEmojiViewModel = hiltViewModel()
    
    val backgroundColor by animateColorAsState(
        targetValue = if (item.isHighlighted) MaterialTheme.colorScheme.primary.copy(
            alpha = 0.1f
        ) else Color.Transparent
    )
    
    if (message.messageType == MessageType.STICKER) {
        val messageSticker = message.sticker
        
        LaunchedEffect(messageSticker?.packId) {
            messageSticker?.let { stickersViewModel.requestPack(it.packId) }
        }
        
        val sticker = messageSticker?.let { stickersState.sticker(it.packId, it.id) }
        
        SwipeToReplyBox(
            enabled = item.canReply && onSwipeToReply != null,
            onReply = { onSwipeToReply?.invoke() },
            onThresholdReached = { onSwipeThresholdReached?.invoke() },
            modifier = modifier.background(backgroundColor)
        ) {
            StickerMessageItem(
                sticker = sticker,
                time = item.time,
                isMine = item.isMine,
                isRead = if (item.isMine && !isSavedMessages) item.isRead else null,
                status = message.status,
                actions = item.dropdownActions,
                onStickerClick = {
                    messageSticker?.let { stickersViewModel.openPack(it.packId) }
                },
                isPinned = isPinned,
                trailingContent = if (onCommentsClick != null) {
                    {
                        CommentCountBadge(
                            count = message.commentsCount,
                            onClick = onCommentsClick
                        )
                    }
                } else {
                    null
                },
                replyContent = message.replyTo?.let { preview ->
                    {
                        ReplyQuote(
                            preview = preview,
                            modifier = Modifier
                                .widthIn(max = 200.dp)
                                .padding(start = 8.dp),
                            onClick = onReplyPreviewClick
                        )
                    }
                }
            )
        }
        
        return
    }
    
    val handleLinkClicked: (String) -> Unit = { url ->
        val stickerPackUsername = StickerLink.parseUsername(url)
        val emojiPackUsername = EmojiLink.parseUsername(url)
        
        when {
            stickerPackUsername != null -> stickersViewModel.openPackByUsername(stickerPackUsername)
            
            emojiPackUsername != null -> emojiViewModel.openPackByUsername(emojiPackUsername)
            
            else -> onLinkClicked?.invoke(url)
        }
    }
    
    val mediaAttachments = remember(message.attachments) {
        message.attachments.filter {
            it.type == AttachmentType.IMAGE || it.type == AttachmentType.VIDEO || it.type == AttachmentType.GIF
        }
    }
    
    val isMediaOnly = message.text.isNullOrBlank() &&
            mediaAttachments.isNotEmpty() &&
            message.attachments.size == mediaAttachments.size
    
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
                    .widthIn(min = 64.dp, max = dynamicMaxWidth)
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

                    if (message.messageType == MessageType.CONTACT) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            MessageContact(
                                message = message,
                                isMine = item.isMine,
                                onViewClick = { onContactClick?.invoke() },
                                modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp)
                            )

                            Box(
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                MessageFooter(
                                    time = item.time,
                                    isRead = if (item.isMine && !isSavedMessages) item.isRead else null,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    status = message.status,
                                    isEdited = message.isEdited,
                                    isPinned = isPinned
                                )
                            }
                        }
                    }

                    if (mediaAttachments.isNotEmpty()) {
                        val mediaSizes = mediaAttachments.map { attachment ->
                            val frameWidth = attachment.width ?: 0
                            val frameHeight = attachment.height ?: 0
                            
                            if (frameWidth > 0 && frameHeight > 0) IntSize(frameWidth, frameHeight)
                            else IntSize.Zero
                        }
                        
                        val mediaCacheKeyPrefix =
                            "$MEDIA_CACHE_KEY_PREFIX:${message.chatId}:${message.senderId}:${message.sendTime}"
                        
                        Box {
                            ImageGridCustomLayout(
                                maxWidth = contentMaxWidth,
                                itemSizes = mediaSizes,
                                content = {
                                    mediaAttachments.forEach { attachment ->
                                        val mediaUri = attachment.localUri
                                        
                                        if (mediaUri == null) {
                                            MediaPlaceholder(
                                                attachment = attachment,
                                                onFileAction = onFileAction
                                            )
                                        } else {
                                            MediaThumbnail(
                                                attachment = attachment,
                                                mediaUri = mediaUri,
                                                cacheKey = "$mediaCacheKeyPrefix:${attachment.fileId}:$mediaUri",
                                                transitionKey = chatMediaKey(
                                                    attachment.messageId,
                                                    mediaUri,
                                                    mediaKeyScope
                                                ),
                                                onFileAction = onFileAction
                                            )
                                        }
                                    }
                                })
                            
                            if (message.text.isNullOrBlank()) {
                                StickerMessageFooter(
                                    time = item.time,
                                    isRead = if (item.isMine && !isSavedMessages) item.isRead else null,
                                    status = message.status,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp),
                                    isPinned = isPinned
                                )
                            }
                        }
                    }
                    
                    Box {
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
                                    if (attachment.extension.isAudioFile()) {
                                        MessageMusic(
                                            file = attachment,
                                            metadata = audioMetadata[attachment.fileId],
                                            isCurrentTrack = currentMusicFileId == attachment.fileId,
                                            isPlaying = isMusicPlaying && currentMusicFileId == attachment.fileId,
                                            positionMs = if (currentMusicFileId == attachment.fileId) musicPositionMs else 0,
                                            durationMs = if (currentMusicFileId == attachment.fileId) musicDurationMs else 0,
                                            onAction = { action ->
                                                onFileAction(attachment, action)
                                            },
                                            onSeek = { positionMs ->
                                                onMusicSeek(attachment, positionMs)
                                            })
                                    } else {
                                        MessageFile(
                                            file = attachment, onAction = { action ->
                                                onFileAction(attachment, action)
                                            })
                                    }
                                }
                                
                                else -> {}
                            }
                        }
                        
                        if (message.text.isNullOrBlank() &&
                            mediaAttachments.isEmpty() &&
                            message.messageType != MessageType.CONTACT
                        ) {
                            MessageFooter(
                                time = item.time,
                                isRead = if (item.isMine && !isSavedMessages) item.isRead else null,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                status = message.status,
                                isEdited = message.isEdited,
                                isPinned = isPinned
                            )
                        }
                    }
                    
                    if (!message.text.isNullOrBlank()) {
                        FlexBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 4.dp),
                            config = {
                                direction(FlexDirection.Row)
                                wrap(FlexWrap.Wrap)
                                justifyContent(FlexJustifyContent.End)
                                gap(4.dp)
                            }) {
                            MessageText(
                                text = message.text,
                                modifier = Modifier
                                    .widthIn(max = contentMaxWidth - 16.dp)
                                    .padding(bottom = 2.dp)
                                    .flex {
                                        grow(1f)
                                    },
                                onLinkClicked = handleLinkClicked,
                                onUsernameClicked = onUsernameClicked,
                                onEmailClicked = onEmailClicked,
                                onPhoneClicked = onPhoneClicked
                            )
                            MessageFooter(
                                time = item.time,
                                isRead = if (item.isMine && !isSavedMessages) item.isRead else null,
                                modifier = Modifier.flex {
                                    alignSelf(FlexAlignSelf.End)
                                },
                                status = message.status,
                                isEdited = message.isEdited,
                                isPinned = isPinned
                            )
                        }
                    }
                    
                    if (onCommentsClick != null) {
                        if (!isMediaOnly) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                            )
                        }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onCommentsClick)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (message.commentsCount > 0) {
                                    pluralStringResource(
                                        R.plurals.comments_count,
                                        message.commentsCount,
                                        message.commentsCount
                                    )
                                } else {
                                    stringResource(R.string.comment)
                                },
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp
                            )
                            Spacer(
                                modifier = Modifier
                                    .widthIn(min = 8.dp)
                                    .weight(1f)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
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
                        
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
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
