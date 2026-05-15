/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aiwazian.messenger.domain.MessageAttachment
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.enums.FileAction
import com.aiwazian.messenger.extensions.formatFileSize
import com.aiwazian.messenger.extensions.sharedElement
import com.aiwazian.messenger.ui.screens.chat.ChatItem

@Composable
fun MessageBubble(
    item: ChatItem.MessageItem,
    onSeen: () -> Unit,
    onFileAction: (MessageAttachment, FileAction) -> Unit,
    onLinkClicked: ((String) -> Unit)? = null,
    onUsernameClicked: ((String) -> Unit)? = null
) {
    val message = item.message
    var expanded by remember { mutableStateOf(false) }
    val alignment = if (item.isMine) Arrangement.End else Arrangement.Start
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(isVisible) {
        if (isVisible && item.isRead == false) {
            onSeen()
        }
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = alignment,
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                val position = coordinates.positionInParent()
                val isElementVisible =
                    position.y >= 0 && position.y < (coordinates.parentLayoutCoordinates?.size?.height
                        ?: 0)
                if (isElementVisible) isVisible = true
            }
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
                .widthIn(
                    min = 60.dp, max = 280.dp
                )
                .padding(horizontal = 4.dp)
                .clip(MaterialTheme.shapes.large)
                .background(containerColor)
        ) {
            Column {
                if (!item.isMine && item.isFirstInGroup && item.senderName != null) {
                    Text(
                        text = item.senderName,
                        fontSize = 12.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp)
                    )
                }
                
                val images = message.attachments.filter { it.type == AttachmentType.IMAGE }
                if (images.isNotEmpty()) {
                    ImageGridCustomLayout(
                        Modifier.heightIn(max = 400.dp), content = {
                            images.forEach { attachment ->
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
                                            },
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
                                    AsyncImage(
                                        model = attachment.localUri,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .sharedElement(key = attachment.localUri)
                                            .fillMaxSize()
                                            .clip(MaterialTheme.shapes.extraSmall)
                                            .clickable(onClick = {
                                                onFileAction(attachment, FileAction.OPEN)
                                            })
                                    )
                                }
                            }
                        })
                }
                
                message.attachments.filter { it.type != AttachmentType.IMAGE }
                    .forEach { attachment ->
                        when (attachment.type) {
                            AttachmentType.VIDEO, AttachmentType.VOICE, AttachmentType.FILE -> {
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
                        onUsernameClicked = onUsernameClicked
                    )
                }
            }
            
            Box(modifier = Modifier.align(Alignment.BottomEnd)) {
                MessageFooter(
                    time = item.time, isRead = if (item.isMine) item.isRead else null
                )
            }
            
            MessageDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                actions = item.dropdownActions
            )
        }
    }
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
        
        val width = constraints.maxWidth
        val height =
            if (constraints.hasBoundedHeight) constraints.maxHeight else (width * 0.75f).toInt()
        
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
    measurables: List<androidx.compose.ui.layout.Measurable>,
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
