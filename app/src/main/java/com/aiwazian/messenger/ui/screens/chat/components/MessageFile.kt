/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiwazian.messenger.domain.DownloadStatus
import com.aiwazian.messenger.domain.MessageFile
import kotlin.math.log10
import kotlin.math.pow

@Composable
fun MessageFile(
    file: MessageFile,
    isMine: Boolean,
    onAction: (FileAction) -> Unit
) {
    val containerColor = if (isMine) MaterialTheme.colorScheme.primaryContainer else Color(0x66646464)
    
    Box(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .background(containerColor, RoundedCornerShape(16.dp))
            .widthIn(max = 280.dp)
            .clickable {
                when (file.status) {
                    DownloadStatus.DOWNLOADING -> onAction(FileAction.PAUSE)
                    DownloadStatus.PAUSED -> onAction(FileAction.RESUME)
                    DownloadStatus.IDLE -> onAction(FileAction.DOWNLOAD)
                    else -> {}
                }
            }
            .padding(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                StatusIcon(file.status, onAction)
                if (file.status == DownloadStatus.DOWNLOADING || file.status == DownloadStatus.UPLOADING) {
                    CircularProgressIndicator(
                        progress = { file.progress / 100f },
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatFileSize(file.size),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (file.status == DownloadStatus.DOWNLOADING || file.status == DownloadStatus.UPLOADING) {
                        Text(
                            text = "${file.progress}%",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            if (file.status == DownloadStatus.UPLOADING) {
                IconButton(onClick = { onAction(FileAction.CANCEL) }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Cancel Upload")
                }
            }
        }
    }
}

@Composable
private fun StatusIcon(status: DownloadStatus, onAction: (FileAction) -> Unit) {
    val icon = when (status) {
        DownloadStatus.IDLE -> Icons.Rounded.Download
        DownloadStatus.DOWNLOADING -> Icons.Rounded.Pause
        DownloadStatus.UPLOADING -> Icons.Rounded.Upload
        DownloadStatus.PAUSED -> Icons.Rounded.PlayArrow
        DownloadStatus.COMPLETED -> Icons.Rounded.FilePresent
        DownloadStatus.FAILED -> Icons.Rounded.Refresh
        DownloadStatus.CANCELLED -> Icons.Rounded.Download
    }
    Icon(icon, contentDescription = status.name, tint = MaterialTheme.colorScheme.primary)
}

private fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
    return String.format("%.1f %s", size / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
}

enum class FileAction {
    DOWNLOAD, PAUSE, RESUME, CANCEL
}
