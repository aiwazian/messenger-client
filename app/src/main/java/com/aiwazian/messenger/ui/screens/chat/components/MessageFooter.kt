/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MessageFooter(
    time: String,
    isRead: Boolean?
) {
    Row(
        modifier = Modifier.padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = time,
            fontSize = 10.sp,
            lineHeight = 10.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        
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
