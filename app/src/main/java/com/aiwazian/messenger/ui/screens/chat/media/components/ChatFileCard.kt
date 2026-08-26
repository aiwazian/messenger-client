/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.media.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import com.aiwazian.messenger.domain.ChatMediaItem
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.extensions.formatFileSize
import com.aiwazian.messenger.extensions.getFileIcon
import com.aiwazian.messenger.extensions.toInstance
import com.aiwazian.messenger.extensions.toPrettyDateWithYear

/**
 * Документ в галерее чата.
 *
 * Сверху название, под ним — размер и дата отправки. Стрелка загрузки
 * стоит рядом с размером, а не отдельной кнопкой справа: качать и
 * открывать — одно и то же нажатие по карточке, и вторая цель только
 * сбивала бы с толку.
 *
 * Полоска прогресса лежит под текстом и появляется только на время
 * загрузки: пустая линейка в полном списке читается как разделитель.
 */
@Composable
fun ChatFileCard(
    file: ChatMediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDownloaded = file.localUri != null
    val isDownloading = file.status == DownloadStatus.DOWNLOADING
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
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
            Icon(
                imageVector = file.extension.getFileIcon(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
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
                lineHeight = 16.sp
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!isDownloaded) {
                    Icon(
                        imageVector = if (isDownloading) {
                            Icons.Rounded.Pause
                        } else {
                            Icons.Rounded.Download
                        },
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = "${file.size.formatFileSize()}, ${file.sendTime.toInstance().toPrettyDateWithYear()}",
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isDownloading) {
                /* До первого процента показываем бегущую: нуль неотличим от зависания. */
                if (file.progress == 0) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else {
                    val animatedProgress by animateFloatAsState(
                        targetValue = file.progress / 100f,
                        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
                    )
                    
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
