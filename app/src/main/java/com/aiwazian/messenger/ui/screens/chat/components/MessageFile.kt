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
import androidx.compose.material.icons.rounded.FilePresent
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import com.aiwazian.messenger.domain.DownloadStatus
import com.aiwazian.messenger.domain.MessageFile
import com.aiwazian.messenger.enums.FileAction
import kotlin.math.log10
import kotlin.math.pow

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MessageFile(
    file: MessageFile,
    onAction: (FileAction) -> Unit
) {
    Row(
        modifier = Modifier
            .clickable {
                when (file.status) {
                    DownloadStatus.DOWNLOADING -> onAction(FileAction.PAUSE)
                    DownloadStatus.PAUSED -> onAction(FileAction.RESUME)
                    DownloadStatus.IDLE -> onAction(FileAction.DOWNLOAD)
                    DownloadStatus.UPLOADING -> onAction(FileAction.CANCEL)
                    DownloadStatus.COMPLETED -> onAction(FileAction.OPEN)
                    DownloadStatus.FAILED -> onAction(FileAction.DOWNLOAD)
                    DownloadStatus.CANCELLED -> onAction(FileAction.DOWNLOAD)
                }
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
            StatusIcon(file.status)
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
                        progress = { animatedProgress },
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
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
                text = "${formatFileSize(file.size)} • ${file.extension.uppercase()}",
                fontSize = 10.sp,
                lineHeight = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusIcon(
    status: DownloadStatus
) {
    val icon = when (status) {
        DownloadStatus.IDLE -> Icons.Rounded.Download
        DownloadStatus.DOWNLOADING -> Icons.Rounded.Pause
        DownloadStatus.UPLOADING -> Icons.Rounded.Upload
        DownloadStatus.PAUSED -> Icons.Rounded.Downloading
        DownloadStatus.COMPLETED -> Icons.Rounded.FilePresent
        DownloadStatus.FAILED -> Icons.Rounded.Refresh
        DownloadStatus.CANCELLED -> Icons.Rounded.Download
    }
    Icon(
        imageVector = icon,
        contentDescription = status.name,
        tint = MaterialTheme.colorScheme.primary
    )
}

private fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf(
        "B",
        "KB",
        "MB",
        "GB",
        "TB"
    )
    val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
    return String.format(
        "%.1f %s",
        size / 1024.0.pow(digitGroups.toDouble()),
        units[digitGroups]
    )
}
