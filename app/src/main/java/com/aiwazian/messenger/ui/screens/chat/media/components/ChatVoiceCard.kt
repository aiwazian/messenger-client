/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.media.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.ChatMediaItem
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.extensions.toInstance
import com.aiwazian.messenger.extensions.toPrettyTime
import com.aiwazian.messenger.extensions.toShortDateIfNotToday
import com.aiwazian.messenger.utils.AmplitudeExtractor
import java.util.Locale

@Composable
fun ChatVoiceCard(
    voice: ChatMediaItem,
    author: String?,
    isPlaying: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDurationResolved: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val localUri = voice.localUri
    val isDownloading = voice.status == DownloadStatus.DOWNLOADING
    
    LaunchedEffect(localUri, voice.durationMs) {
        if (localUri == null || voice.durationMs != null) {
            return@LaunchedEffect
        }
        
        val analysis = runCatching { AmplitudeExtractor.extract(context, localUri) }.getOrNull()
        val durationMs = analysis?.durationMs ?: return@LaunchedEffect
        
        if (durationMs > 0) {
            onDurationResolved(durationMs)
        }
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            SelectionCheckBadge(
                visible = isSelected,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val sendTime = voice.sendTime.toInstance()
            val date = sendTime.toShortDateIfNotToday()
            val time = sendTime.toPrettyTime()
            
            Text(
                text = if (date == null) {
                    stringResource(R.string.chat_voice_sent_at, time)
                } else {
                    stringResource(R.string.chat_voice_sent_on, date, time)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 16.sp
            )
            
            /*
             * Пока файла нет, длины тоже нет: вторая строка обходится одним автором,
             * а не показывает прочерк вместо времени.
             */
            val details = listOfNotNull(
                author?.takeIf { it.isNotBlank() },
                voice.durationMs?.let { formatVoiceDuration(it) })
            
            if (details.isNotEmpty()) {
                Text(
                    text = details.joinToString(" \u00b7 "),
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isDownloading) {
                if (voice.progress == 0) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else {
                    val animatedProgress by animateFloatAsState(
                        targetValue = voice.progress / 100f,
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

private fun formatVoiceDuration(durationMs: Int): String {
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
