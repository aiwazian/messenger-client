/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.dp
import com.aiwazian.messenger.domain.MessageFile
import com.aiwazian.messenger.ui.screens.chat.ChatItem

@Composable
fun MessageBubble(
    item: ChatItem.MessageItem,
    onSeen: () -> Unit,
    onFileAction: (MessageFile, FileAction) -> Unit
) {
    val message = item.message
    var expanded by remember { mutableStateOf(false) }
    val alignment = if (item.isMine) Alignment.Companion.CenterEnd else Alignment.CenterStart
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(isVisible) {
        if (isVisible && item.isRead == false) {
            onSeen()
        }
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                val position = coordinates.positionInParent()
                val isElementVisible =
                    position.y >= 0 && position.y < (coordinates.parentLayoutCoordinates?.size?.height
                        ?: 0)
                if (isElementVisible) isVisible = true
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp,
                    vertical = 1.dp
                )
                .combinedClickable(
                    onClick = { expanded = true },
                    onLongClick = { },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ),
            contentAlignment = alignment
        ) {
            Column(
                horizontalAlignment = if (item.isMine) Alignment.End else Alignment.Start
            ) {
                if (message.files.isNotEmpty()) {
                    message.files.forEach { file ->
                        MessageFile(
                            file = file,
                            isMine = item.isMine,
                            onAction = { action -> onFileAction(file, action) }
                        )
                    }
                }

                if (!message.text.isNullOrBlank()) {
                    MessageText(
                        message.text,
                        item.time,
                        item.isRead,
                        alignment,
                        item.senderName,
                        item.dropdownActions
                    )
                }
            }
        }
        
        MessageDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            actions = item.dropdownActions
        )
    }
}
