/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiwazian.messenger.domain.MessageAttachment
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.enums.FileAction
import com.aiwazian.messenger.extensions.formatFileSize
import com.aiwazian.messenger.extensions.getFileIcon

@Composable
fun MessageFile(file: MessageAttachment, onAction: (FileAction) -> Unit) {
    Row(
        modifier = Modifier
            .clickable {
                if (file.localUri != null) {
                    onAction(FileAction.OPEN)
                    return@clickable
                }
                
                val action = when (file.status) {
                    DownloadStatus.DOWNLOADING -> FileAction.PAUSE
                    DownloadStatus.PAUSED -> FileAction.DOWNLOAD
                    DownloadStatus.IDLE,
                    DownloadStatus.CANCELLED,
                    DownloadStatus.FAILED,
                    DownloadStatus.UPLOADED -> FileAction.DOWNLOAD
                    
                    DownloadStatus.UPLOADING -> FileAction.CANCEL
                    DownloadStatus.COMPLETED -> FileAction.OPEN
                }
                onAction(action)
            }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            if (file.localUri == null && file.status != DownloadStatus.UPLOADING && file.status != DownloadStatus.DOWNLOADING) {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                StatusIcon(file)
                if (file.status == DownloadStatus.DOWNLOADING || file.status == DownloadStatus.UPLOADING) {
                    if (file.progress == 0) {
                        CircularWavyProgressIndicator(
                            modifier = Modifier.size(48.dp)
                        )
                    } else {
                        val animatedProgress by animateFloatAsState(
                            targetValue = file.progress / 100f,
                            animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                        )
                        
                        CircularWavyProgressIndicator(
                            progress = { animatedProgress }, modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
        }
        
        Column(
            modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = file.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 14.sp
            )
            
            Text(
                text = "${file.size.formatFileSize()} • ${file.extension.uppercase()}",
                fontSize = 10.sp,
                lineHeight = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusIcon(
    file: MessageAttachment
) {
    val icon = when (file.status) {
        DownloadStatus.IDLE -> Icons.Rounded.Download
        DownloadStatus.DOWNLOADING -> Icons.Rounded.Pause
        DownloadStatus.UPLOADING -> Icons.Rounded.Upload
        DownloadStatus.PAUSED -> Icons.Rounded.Downloading
        DownloadStatus.COMPLETED -> file.extension.getFileIcon()
        DownloadStatus.FAILED -> Icons.Rounded.Refresh
        DownloadStatus.CANCELLED -> Icons.Rounded.Download
        DownloadStatus.UPLOADED -> Icons.Rounded.Download
    }
    Icon(
        imageVector = icon,
        contentDescription = file.status.name,
        tint = MaterialTheme.colorScheme.primary
    )
}
