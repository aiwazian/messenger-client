/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.MessageStatus

@Composable
fun MessageFooter(
    time: String,
    isRead: Boolean?,
    modifier: Modifier = Modifier,
    status: MessageStatus = MessageStatus.SENT,
    isEdited: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (isEdited) {
            Text(
                text = stringResource(R.string.edited).lowercase(),
                fontSize = 10.sp,
                lineHeight = 10.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Text(
            text = time,
            fontSize = 10.sp,
            lineHeight = 10.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        when (status) {
            MessageStatus.SENDING -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(8.dp),
                    strokeWidth = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            MessageStatus.ERROR -> {
                Icon(
                    Icons.Rounded.Error,
                    null,
                    Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
            
            MessageStatus.SENT -> {
                AnimatedContent(
                    targetState = isRead,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    }) { isRead ->
                    if (isRead == true) {
                        Icon(
                            Icons.Rounded.DoneAll,
                            null,
                            Modifier.size(12.dp)
                        )
                    } else if (isRead == false) {
                        Icon(
                            Icons.Rounded.Done,
                            null,
                            Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}
