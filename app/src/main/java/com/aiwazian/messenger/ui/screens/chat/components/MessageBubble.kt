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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import kotlin.math.roundToInt

private val MediaGridFallbackWidth = 264.dp

/**
 * Границы высоты сетки вложений.
 *
 * Максимум держит высокие кадры в разумных рамках: скриншот в полный рост
 * иначе занял бы полэкрана. Минимум — для обратной крайности: панорама
 * выродилась бы в полоску, в которую не попасть пальцем.
 */
private val MediaGridMinHeight = 80.dp
private val MediaGridMaxHeight = 400.dp

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
                    onClick = { if (showContextMenu) expanded = true },
                    onLongClick = { },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() })
        ) {
            val containerColor =
                if (item.isMine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
            
            Box(
                modifier = Modifier
                    .widthIn(min = 90.dp, max = 300.dp)
                    .padding(horizontal = 8.dp)
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
                        /*
                         * Форма единственного вложения известна до загрузки картинки:
                         * размеры кадра приходят вместе с файлом. По ним карточка
                         * заранее получает нужную высоту — квадрат под квадратной
                         * картинкой, широкая полоса под горизонтальным видео, — и лента
                         * не прыгает в момент, когда картинка догрузилась.
                         *
                         * У нескольких вложений соотношение не считается: там сетка
                         * делит общую площадь между плитками, и одно из вложений
                         * форму остальным навязывать не должно.
                         *
                         * Пустые размеры — штатный случай: у медиа, отправленного
                         * старым клиентом, их нет, и карточка рисуется по-старому.
                         */
                        val singleMediaRatio = mediaAttachments.singleOrNull()?.let { attachment ->
                            val width = attachment.width ?: return@let null
                            val height = attachment.height ?: return@let null
                            
                            if (width <= 0 || height <= 0) null
                            else width.toFloat() / height.toFloat()
                        }
                        
                        ImageGridCustomLayout(
                            Modifier.heightIn(max = MediaGridMaxHeight),
                            singleItemAspectRatio = singleMediaRatio,
                            content = {
                                mediaAttachments.forEach { attachment ->
                                    if (attachment.localUri == null ||
                                        attachment.status == DownloadStatus.UPLOADING ||
                                        attachment.status == DownloadStatus.DOWNLOADING
                                    ) {
                                        /*
                                         * Место под картинку видно до самой картинки:
                                         * подложка заливается цветом, иначе на месте
                                         * вложения висел бы один индикатор посреди пузыря
                                         * и форма карточки не читалась.
                                         */
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
                                                    Icon(Icons.Rounded.