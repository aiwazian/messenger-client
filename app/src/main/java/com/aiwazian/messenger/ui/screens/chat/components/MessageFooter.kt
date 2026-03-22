/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MessageFooter(
    time: String,
    isRead: Boolean?
) {
    Row(
        modifier = Modifier.Companion
            .fillMaxWidth()
            .padding(
                end = 8.dp,
                bottom = 4.dp
            ),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Companion.Bottom
    ) {
        Text(
            text = time,
            style = TextStyle(textAlign = TextAlign.Companion.Center),
            fontSize = 9.sp,
            color = Color.Companion.White
        )
        
        if (isRead != true) {
            Icon(
                Icons.Rounded.DoneAll,
                null,
                Modifier.size(16.dp)
            )
        } else {
            Icon(
                Icons.Rounded.Done,
                null,
                Modifier.size(16.dp)
            )
        }
    }
}
