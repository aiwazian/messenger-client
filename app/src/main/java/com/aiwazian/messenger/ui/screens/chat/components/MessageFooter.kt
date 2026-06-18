/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiwazian.messenger.R

@Composable
fun MessageFooter(
    time: String,
    isRead: Boolean?,
    isEdited: Boolean = false
) {
    Row(
        modifier = Modifier.padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isEdited) {
            Text(
                text = stringResource(R.string.edited),
                fontSize = 10.sp,
                lineHeight = 10.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.size(2.dp))
        }
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
