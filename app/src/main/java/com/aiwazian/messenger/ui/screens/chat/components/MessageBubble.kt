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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
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
import com.aiwazian.messenger.ui.components.formatDuration
import com.aiwazian.messenger.ui.components.topBar.DropdownMenuAction
import com.aiwazian.messenger.ui.screens.chat.ChatItem
import com.aiwazian.messenger.utils.UiText
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Ширина сетки медиа, когда родитель спрашивает размер без ограничений
 * (intrinsic-измерение из-за `Modifier.width(IntrinsicSize.Max)` у колонки пузыря).
 * Соответствует максимальной ширине пузыря за вычетом горизонтальных отступов.
 */
private val MediaGridFallbackWidth = 264.dp

/** Предел размера лейаута в Compose: больше — падает IllegalStateException. */
private const val MaxLayoutDimension = 16_777_215

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
    /** Клик по почтовому адресу в тексте. */
    onEmailClicked: ((String) -> Unit)? = null,
    onSaveToDownloads: (() -> Unit)? = null,
    /** Клик по цитате: прыжок к оригиналу или переход в чат оригинала. */
    onReplyPreviewClick: (() -> Unit)? = null,
    /** Клик по заголовку «Переслано от». */
    onForwardedFromClick: (() -> Unit)? = null,
    /** Свайп влево пересёк порог: короткая тактильная отдача. */
    onSwipeThresholdReached: (() -> Unit)? = null,
    /** Палец отпущен за порогом: начинаем ответ на это сообщение. */
    onSwipeToReply: (() -> Unit)? = null
) {
    val message = item.message
    var expanded by remember { mutableStateOf(false) }
    var showReadersDropdown by remember { mutableStateOf(false) }
    val alignment = if (item.isMine) Arrangement.End else Arrangement.Start
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = alignment,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { expanded = true },
                    onLongClick = { },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() })
        ) {
            val containerColor =
                if (item.isMine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
            
            Box(
                modifier = Modifier
                    .widthIn(min = 90.dp, max = 280.dp)
                    .padding(horizontal = 8.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(containerColor)
            ) {
                Column(Modifier.width(IntrinsicSize.Max)) {
                    if (!item.isMine && item.isFirstInGroup && item.senderName != null) {
                        Text(
                            text = item.senderName,
                            fontSize = 12.sp,
                            lineHeight = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
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
                        ImageGridCustomLayout(
                            Modifier.heightIn(max = 400.dp), content = {
                                mediaAttachments.forEach { attachment ->
                                    if (attachment.localUri == null ||
                                        attachment.status == DownloadStatus.UPLOADING ||
                                        attachment.status == DownloadStatus.DOWNLOADING
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(MaterialTheme.shapes.extraSmall)
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
                        isEdited = message.editedAt != null
                    )
                }
                
                MessageDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    actions = buildDropdownActions(item, isSavedMessages, onSaveToDownloads) {
                        showReadersDropdown = true
                    }
                )
                
                val readers = item.readInfo.orEmpty()
                if (readers.isNotEmpty()) {
                    AppDropdownMenu(
                        expanded = showReadersDropdown,
                        onDismissRequest = { showReadersDropdown = false },
                        properties = PopupProperties(focusable = true)
                    ) {
                        readers.forEach { reader ->
                            val name = listOfNotNull(reader.firstName, reader.lastName)
                                .joinToString(" ").ifEmpty { reader.userId.toString() }
                            val readTime = reader.readAt.toInstance().toPrettyTime()
                            DropdownMenuItem(
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = reader.firstName.firstOrNull()?.uppercase()
                                                ?: "?",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                text = {
                                    Column {
                                        Text(
                                            text = name,
                                            fontSize = 14.sp,
                                            lineHeight = 18.sp
                                        )
                                        Text(
                                            text = readTime,
                                            fontSize = 12.sp,
                                            lineHeight = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = { showReadersDropdown = false }
                            )
                        }
                    }
                }
            }
        }
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
        val editedTime = item.message.editedAt.toInstance().toPrettyTime()
        val editedDate = item.message.editedAt.toInstance().atZone(ZoneId.systemDefault())
        val now = LocalDate.now()
        val label = if (editedDate.toLocalDate() == now) {
            "Изменено в $editedTime"
        } else {
            "Изменено " + editedDate.format(DateTimeFormatter.ofPattern("d MMMM HH:mm"))
        }
        actions.add(
            DropdownMenuAction(
                icon = Icons.Rounded.EditCalendar,
                text = UiText.DynamicString(label),
                onClick = null
            )
        )
    }
    
    if (item.isMine && !isSavedMessages) {
        val readInfo = item.readInfo
        val isRead = item.isRead
        
        if (item.chatType == ChatType.PRIVATE && isRead == true) {
            val now = LocalDate.now()
            val msgDate = item.message.sendTime.toInstance().atZone(ZoneId.systemDefault())
                .toLocalDate()
            val label = if (msgDate == now) {
                "Прочитано в " + item.message.sendTime.toInstance().toPrettyTime()
            } else {
                "Прочитано " + item.message.sendTime.toInstance().atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("d MMMM HH:mm"))
            }
            actions.add(
                DropdownMenuAction(
                    icon = Icons.Rounded.DoneAll,
                    text = UiText.DynamicString(label),
                    onClick = null
                )
            )
        } else if (item.chatType == ChatType.GROUP && !readInfo.isNullOrEmpty()) {
            val count = readInfo.size
            val word = when {
                count % 10 == 1 && count % 100 != 11 -> "просмотр"
                count % 10 in 2..4 && count % 100 !in 12..14 -> "просмотра"
                else -> "просмотров"
            } // TODO plural string
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
                icon = Icons.Rounded.Download,
                text = UiText.StringResource(R.string.save_to_downloads),
                onClick = onSaveToDownloads
            )
        )
    }
    
    actions.addAll(item.dropdownActions)
    return actions
}

@Composable
fun ImageGridCustomLayout(
    modifier: Modifier = Modifier, spacing: Dp = 2.dp, content: @Composable () -> Unit
) {
    val gap = with(LocalDensity.current) { spacing.toPx() }
    
    Layout(
        content = content, modifier = modifier
            .padding(spacing)
            .clip(MaterialTheme.shapes.large)
    ) { measurables, constraints ->
        val count = measurables.size.coerceAtMost(10)
        if (count == 0) return@Layout layout(0, 0) {}
        
        /*
         * При intrinsic-измерении (колонка пузыря использует IntrinsicSize.Max)
         * ограничения приходят бесконечными: maxWidth == Int.MAX_VALUE.
         * Без этого fallback лейаут пытался сообщить размер 2147483647 и падал
         * с IllegalStateException: Size(...) is out of range.
         */
        val width = (if (constraints.hasBoundedWidth) constraints.maxWidth
        else MediaGridFallbackWidth.roundToPx()).coerceIn(0, MaxLayoutDimension)
        
        val height = (if (constraints.hasBoundedHeight) constraints.maxHeight
        else (width * 0.75f).toInt()).coerceIn(0, MaxLayoutDimension)
        
        layout(width, height) {
            when (count) {
                1 -> {
                    val p = measurables[0].measure(Constraints.fixed(width, height))
                    p.place(0, 0)
                }
                
                2 -> {
                    val itemW = (width - gap) / 2
                    val itemConstraints = Constraints.fixed(itemW.toInt(), height)
                    
                    measurables.take(2).forEachIndexed { i, m ->
                        val p = m.measure(itemConstraints)
                        p.place((i * (itemW + gap)).toInt(), 0)
                    }
                }
                
                3 -> {
                    val mainW = (width * 0.6f).toInt()
                    val sideW = (width - mainW - gap).toInt()
                    val sideH = (height - gap) / 2
                    
                    val p1 = measurables[0].measure(Constraints.fixed(mainW, height))
                    val p2 = measurables[1].measure(Constraints.fixed(sideW, sideH.toInt()))
                    val p3 = measurables[2].measure(Constraints.fixed(sideW, sideH.toInt()))
                    
                    p1.place(0, 0)
                    p2.place(mainW + gap.toInt(), 0)
                    p3.place(mainW + gap.toInt(), (sideH + gap).toInt())
                }
                
                4 -> {
                    val itemW = (width - gap) / 2
                    val itemH = (height - gap) / 2
                    val itemConstraints = Constraints.fixed(itemW.toInt(), itemH.toInt())
                    
                    measurables.take(4).forEachIndexed { i, m ->
                        val p = m.measure(itemConstraints)
                        val x = (i % 2) * (itemW + gap)
                        val y = (i / 2) * (itemH + gap)
                        p.place(x.toInt(), y.toInt())
                    }
                }
                
                5 -> {
                    val rows = listOf(2, 3)
                    placeGrid(measurables, rows, width, height, gap)
                }
                
                6 -> {
                    val rows = listOf(3, 3)
                    placeGrid(measurables, rows, width, height, gap)
                }
                
                7 -> {
                    val rows = listOf(4, 3)
                    placeGrid(measurables, rows, width, height, gap)
                }
                
                8 -> {
                    val rows = listOf(2, 3, 3)
                    placeGrid(measurables, rows, width, height, gap)
                }
                
                9 -> {
                    val rows = listOf(3, 3, 3)
                    placeGrid(measurables, rows, width, height, gap)
                }
                
                10 -> {
                    val rows = listOf(3, 4, 3)
                    placeGrid(measurables, rows, width, height, gap)
                }
                
                else -> {
                    val columns = 3
                    val rowsCount = (count + columns - 1) / columns
                    val itemW = (width - gap * (columns - 1)) / columns
                    val itemH = (height - gap * (rowsCount - 1)) / rowsCount
                    val itemConstraints = Constraints.fixed(itemW.toInt(), itemH.toInt())
                    
                    measurables.take(count).forEachIndexed { i, m ->
                        val p = m.measure(itemConstraints)
                        val x = (i % columns) * (itemW + gap)
                        val y = (i / columns) * (itemH + gap)
                        p.place(x.toInt(), y.toInt())
                    }
                }
            }
        }
    }
}

private fun Placeable.PlacementScope.placeGrid(
    measurables: List<Measurable>,
    rows: List<Int>,
    totalWidth: Int,
    totalHeight: Int,
    gap: Float
) {
    var currentIndex = 0
    val rowCount = rows.size
    val rowH = (totalHeight - gap * (rowCount - 1)) / rowCount
    
    rows.forEachIndexed { rowIndex, itemCount ->
        val y = (rowIndex * (rowH + gap)).toInt()
        val rowW = (totalWidth - gap * (itemCount - 1)) / itemCount
        
        for (i in 0 until itemCount) {
            if (currentIndex < measurables.size) {
                val p =
                    measurables[currentIndex].measure(Constraints.fixed(rowW.toInt(), rowH.toInt()))
                val x = (i * (rowW + gap)).toInt()
                p.place(x, y)
                currentIndex++
            }
        }
    }
}

@Composable
private fun VideoThumbnail(videoUri: Uri, onClick: () -> Unit) {
    val context = LocalContext.current
    
    Box(Modifier.clickable(onClick = onClick)) {
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
    
    Box(Modifier.clickable(onClick = onClick)) {
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
