/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.utils.VibrationPattern

@Composable
fun SwipeableChatCard(
    chat: Chat,
    selected: Boolean = false,
    pinned: Boolean = false,
    enableSwipeable: Boolean = true,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onDismiss: () -> Unit,
    backgroundIcon: ImageVector? = null,
    onVibrate: (LongArray) -> Unit
) {
    val swipeToDismissBoxState = rememberSwipeToDismissBoxState()
    
    val backgroundColor by animateColorAsState(
        targetValue = if (swipeToDismissBoxState.progress > 0.5f && swipeToDismissBoxState.progress < 1f) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.primary
        }
    )
    
    var leftVibration by remember { mutableStateOf(false) }
    
    if (swipeToDismissBoxState.progress > 0.5f && swipeToDismissBoxState.progress < 0.9f && !leftVibration) {
        onVibrate(VibrationPattern.TactileResponse)
        leftVibration = true
    } else if (swipeToDismissBoxState.progress == 1f && leftVibration) {
        onVibrate(VibrationPattern.TactileResponse)
        leftVibration = false
    }
    
    SwipeToDismissBox(
        onDismiss = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDismiss()
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = false,
        state = swipeToDismissBoxState,
        backgroundContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(backgroundColor)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.End
            ) {
                if (backgroundIcon != null) {
                    Icon(
                        imageVector = backgroundIcon,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }) {
        ChatCard(
            chat = chat,
            isSelected = selected,
            onClickChat = onClick,
            onLongClickChat = onLongClick
        )
    }
}
