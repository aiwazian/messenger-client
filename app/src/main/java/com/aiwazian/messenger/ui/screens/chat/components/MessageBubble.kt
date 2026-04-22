/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiwazian.messenger.domain.MessageFile
import com.aiwazian.messenger.enums.FileAction
import com.aiwazian.messenger.ui.screens.chat.ChatItem

@Composable
fun MessageBubble(
    item: ChatItem.MessageItem,
    onSeen: () -> Unit,
    onFileAction: (MessageFile, FileAction) -> Unit,
    onLinkClicked: ((String) -> Unit)? = null
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
                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
        val containerColor =
            if (item.isMine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
        
        Box(
            modifier = Modifier
                .widthIn(
                    min = 60.dp,
                    max = 280.dp
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
                
                message.attachments.forEach { file ->
                    MessageFile(
                        file = file,
                        onAction = { action ->
                            onFileAction(
                                file,
                                action
                            )
                        }
                    )
                }
                
                if (!message.text.isNullOrBlank()) {
                    MessageText(message.text, onLinkClicked = onLinkClicked)
                }
            }
            
            Box(modifier = Modifier.align(Alignment.BottomEnd)) {
                MessageFooter(
                    time = item.time,
                    isRead = if (item.isMine) item.isRead else null
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
