/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiwazian.messenger.domain.DropdownMenuAction

@Composable
fun MessageText(
    text: String,
    time: String,
    isRead: Boolean?,
    alignment: Alignment,
    senderName: String?,
    actions: List<DropdownMenuAction>
) {
    Box(
        modifier = Modifier
            .background(
                color = if (alignment == Alignment.Companion.CenterEnd) MaterialTheme.colorScheme.primaryContainer else Color(0x66646464),
                shape = RoundedCornerShape(16.dp)
            )
            .widthIn(max = 280.dp)
            .padding(top = 6.dp)
    ) {
        Column {
            senderName?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.W500,
                    lineHeight = 10.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
            
            AnnotatedTextMessage(text)
        }
        
        MessageFooter(
            time,
            isRead
        )
    }
}
