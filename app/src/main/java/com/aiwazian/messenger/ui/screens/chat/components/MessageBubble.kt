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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val BubbleHorizontalPadding = 8.dp

private const val MEDIA_CACHE_KEY_PREFIX = "chat-media"

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
    onSaveToDownloads: (() -> Unit)? = null,
    onReplyPreviewClick: (() -> Unit)? = null,
    onForwardedFromClick: (() -> Unit)? = null,
    onSwipeThresholdReached: (() -> Unit)? = null,
    onSwipeToReply: (() -> Unit)? = null,
    readerAvatars: Map<Long, Uri?> = emptyMap(),
    onReadersRequested: ((List<Long>) -> Unit)? = null,
    onSenderNameClick: (() -> Unit)? = null,
    onReaderClick: ((MessageReadInfo) -> Unit)? = null,
    showContextMenu: Boolean = true,
    isPinned: Boolean = false
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
                isPinned = isPinned
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
                    
                    if (mediaAttachments.isNotEmpty()) {
                        val mediaSizes = mediaAttachments.map { attachment ->
                            val frameWidth = attachment.width ?: 0
                            val frameHeight = attachment.height ?: 0
                            
                            if (frameWidth > 0 && frameHeight > 0) IntSize(frameWidth, frameHeight)
                            else IntSize.Zero
                        }
                        
                        val mediaCacheKeyPrefix =
                            "$MEDIA_CACHE_KEY_PREFIX:${message.chatId}:${message.senderId}:${message.sendTime}"
                        
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
                                            cacheKey = "$mediaCacheKeyPrefix:${attachment.fileId}",
                                            transitionKey = chatMediaKey(attachment.messageId, mediaUri),
                                            onFileAction = onFileAction
                                        )
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
                                onEmailClicked = onEmailClicked
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
                }
                
                if (message.text.isNullOrBlank()) {
                    Box(modifier = Modifier.align(Alignment.BottomEnd)) {
                        if (mediaAttachments.isNotEmpty()) {
                            StickerMessageFooter(
                                time = item.time,
                                isRead = if (item.isMine && !isSavedMessages) item.isRead else null,
                                status = message.status,
                                modifier = Modifier.padding(4.dp),
                                isPinned = isPinned
                            )
                        } else {
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

private fun DownloadStatus.isTransferring(): Boolean {
    return this == DownloadStatus.UPLOADING ||
            this == DownloadStatus.DOWNLOADING ||
            this == DownloadStatus.PAUSED
}

private fun DownloadStatus.toFileAction(): FileAction {
    return if (this == DownloadStatus.UPLOADING) FileAction.CANCEL else FileAction.DOWNLOAD
}

@Composable
private fun MediaPlaceholder(
    attachment: MessageAttachment,
    onFileAction: (MessageAttachment, FileAction) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .clickable { onFileAction(attachment, attachment.status.toFileAction()) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = attachment.size.formatFileSize(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp),
            fontSize = 12.sp,
            lineHeight = 12.sp
        )
        
        MediaStatusIndicator(attachment.status)
    }
}

@Composable
private fun MediaThumbnail(
    attachment: MessageAttachment,
    mediaUri: Uri,
    cacheKey: String,
    transitionKey: String,
    onFileAction: (MessageAttachment, FileAction) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (attachment.type == AttachmentType.VIDEO) {
            VideoThumbnail(videoUri = mediaUri, cacheKey = cacheKey, transitionKey = transitionKey) {
                onFileAction(attachment, FileAction.OPEN)
            }
        } else {
            ImageThumbnail(
                imageUri = mediaUri,
                cacheKey = cacheKey,
                transitionKey = transitionKey,
                isGif = attachment.type == AttachmentType.GIF
            ) {
                onFileAction(attachment, FileAction.OPEN)
            }
        }
        
        if (attachment.status.isTransferring()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f))
                    .clickable { onFileAction(attachment, attachment.status.toFileAction()) },
                contentAlignment = Alignment.Center
            ) {
                MediaStatusIndicator(attachment.status)
            }
        }
    }
}

@Composable
private fun MediaStatusIndicator(status: DownloadStatus) {
    when (status) {
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

@Composable
private fun VideoThumbnail(videoUri: Uri, cacheKey: String, transitionKey: String, onClick: () -> Unit) {
    val context = LocalContext.current
    val decoderFactory = remember { VideoFrameDecoder.Factory() }

    val request = remember(context, videoUri, cacheKey, decoderFactory) {
        ImageRequest.Builder(context)
            .data(videoUri)
            .decoderFactory(decoderFactory)
            .videoFrameMillis(0)
            .memoryCacheKey(cacheKey)
            .placeholderMemoryCacheKey(cacheKey)
            .build()
    }

    val duration by produceState(0L, videoUri) {
        value = withContext(Dispatchers.IO) { videoUri.getDuration(context) }
    }

    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .mediaTransitionOrigin(transitionKey)
    ) {
        AsyncImage(
            model = request,
            contentDescription = "Thumbnail of the video",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .sharedElement(key = transitionKey)
                .fillMaxSize()
                .clip(MaterialTheme.shapes.extraSmall)
        )
        if (duration > 0) {
            Box(
                modifier = Modifier
                    .padding(start = 4.dp, top = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f))
            ) {
                Text(
                    text = formatDuration(duration),
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

@Composable
private fun ImageThumbnail(
    imageUri: Uri,
    cacheKey: String,
    transitionKey: String,
    isGif: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val decoderFactory = remember { GifDecoder.Factory() }

    val request = remember(context, imageUri, cacheKey, decoderFactory) {
        ImageRequest.Builder(context)
            .data(imageUri)
            .decoderFactory(decoderFactory)
            .memoryCacheKey(cacheKey)
            .placeholderMemoryCacheKey(cacheKey)
            .build()
    }

    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .mediaTransitionOrigin(transitionKey)
    ) {
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .sharedElement(key = transitionKey)
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
