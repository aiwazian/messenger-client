/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiwazian.messenger.extensions.sharedElement

@Composable
fun ProfileCard(
    modifier: Modifier = Modifier,
    id: Long,
    headlineText: String,
    avatarUri: Uri? = null,
    supportingText: String? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(
                text = headlineText,
                maxLines = 1,
                fontSize = 16.sp,
                lineHeight = 16.sp,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.sharedElement(key = "chat-name-$id")
            )
        },
        supportingContent = if (supportingText != null) {
            {
                Text(
                    text = supportingText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else {
            null
        },
        leadingContent = {
            ChatAvatar(
                id = id,
                chatName = headlineText,
                avatarUri = avatarUri,
                size = 40.dp
            )
        },
        trailingContent = trailingContent
    )
}
