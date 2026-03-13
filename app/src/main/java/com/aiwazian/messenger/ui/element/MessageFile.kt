package com.aiwazian.messenger.ui.element

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.automirrored.rounded.TextSnippet
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Css
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Gif
import androidx.compose.material.icons.rounded.Html
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Javascript
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Php
import androidx.compose.material.icons.rounded.VideoCameraBack
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.enums.FileType

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MessageFile(
    fileName: String,
    fileInfo: String,
    onClick: () -> Unit,
    fileType: FileType,
    downloadStatus: DownloadStatus,
    progress: Int = 0
) {
    Row(
        modifier = Modifier
            .clickable {
                onClick.invoke()
            }
            .padding(8.dp)
            .widthIn(min = 220.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onPrimary)
                .padding(4.dp)
                .width(50.dp)
                .height(50.dp),
            contentAlignment = Alignment.Center
        ) {
            when (downloadStatus) {
                DownloadStatus.PENDING -> {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                DownloadStatus.DOWNLOADING -> {
                    CircularWavyProgressIndicator(
                        progress = {
                            progress / 100f
                        },
                        modifier = Modifier
                            .width(46.dp)
                            .height(46.dp)
                    )
                    
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                DownloadStatus.COMPLETED -> {
                    val icon = when (fileType) {
                        FileType.IMAGE -> Icons.Rounded.Image
                        FileType.VIDEO -> Icons.Rounded.VideoCameraBack
                        FileType.MUSIC -> Icons.Rounded.MusicNote
                        FileType.ZIP -> Icons.Rounded.FolderZip
                        FileType.APK -> Icons.Rounded.Android
                        FileType.CSS -> Icons.Rounded.Css
                        FileType.HTML -> Icons.Rounded.Html
                        FileType.JAVASCRIPT -> Icons.Rounded.Javascript
                        FileType.PHP -> Icons.Rounded.Php
                        FileType.GIF -> Icons.Rounded.Gif
                        FileType.TEXT -> Icons.AutoMirrored.Rounded.TextSnippet
                        FileType.JSON -> Icons.Rounded.DataObject
                        FileType.OTHER -> Icons.AutoMirrored.Rounded.InsertDriveFile
                    }
                    
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        Column {
            Text(
                text = fileName,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                fontSize = 16.sp,
                lineHeight = 16.sp
            )
            Text(
                text = fileInfo,
                fontSize = 12.sp,
                lineHeight = 12.sp
            )
        }
    }
}
