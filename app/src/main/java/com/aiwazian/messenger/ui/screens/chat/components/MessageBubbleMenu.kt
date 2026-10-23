/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.extensions.toInstance
import com.aiwazian.messenger.extensions.toPrettyTime
import com.aiwazian.messenger.ui.components.topBar.DropdownMenuAction
import com.aiwazian.messenger.ui.screens.chat.ChatItem
import com.aiwazian.messenger.utils.UiText
import com.aiwazian.messenger.utils.compactCount
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal fun formatStatusTime(timestamp: Long, todayVerb: String): String {
    val instant = timestamp.toInstance()
    val date = instant.atZone(ZoneId.systemDefault())
    val today = LocalDate.now()
    val time = instant.toPrettyTime()

    return when (date.toLocalDate()) {
        today -> "$todayVerb в $time"
        today.minusDays(1) -> "вчера в $time"
        else -> date.format(DateTimeFormatter.ofPattern("d MMMM")) + " в " + time
    }
}

@Composable
internal fun CommentCountBadge(
    count: Int,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(4.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f))
            .padding(8.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.ChatBubble,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )

        if (count > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = compactCount(count),
                fontSize = 11.sp,
                lineHeight = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

internal fun buildDropdownActions(
    item: ChatItem.MessageItem,
    isSavedMessages: Boolean,
    onSaveToDownloads: (() -> Unit)? = null,
    onReadCountClick: () -> Unit = {}
): List<DropdownMenuAction> {
    val actions = mutableListOf<DropdownMenuAction>()

    if (item.message.editedAt != null) {
        actions.add(
            DropdownMenuAction(
                icon = Icons.Rounded.EditCalendar,
                text = UiText.DynamicString(
                    formatStatusTime(item.message.editedAt, todayVerb = "Изменено")
                ),
                onClick = null
            )
        )
    }

    if (item.isMine && !isSavedMessages) {
        val readInfo = item.readInfo

        if (item.chatType == ChatType.PRIVATE) {
            val readAt = readInfo?.maxOfOrNull { it.readAt }

            if (readAt != null) {
                actions.add(
                    DropdownMenuAction(
                        icon = Icons.Rounded.DoneAll,
                        text = UiText.DynamicString(
                            formatStatusTime(readAt, todayVerb = "Прочитано")
                        ),
                        onClick = null
                    )
                )
            }
        } else if (item.chatType == ChatType.GROUP && !readInfo.isNullOrEmpty()) {
            val count = readInfo.size
            val word = when {
                count % 10 == 1 && count % 100 != 11 -> "просмотр"
                count % 10 in 2..4 && count % 100 !in 12..14 -> "просмотра"
                else -> "просмотров"
            }
            actions.add(
                DropdownMenuAction(
                    icon = Icons.Rounded.DoneAll,
                    text = UiText.DynamicString("$count $word"),
                    onClick = onReadCountClick
                )
            )
        }
    }

    val hasDownloadedAttachment = item.message.attachments.any { attachment ->
        !attachment.viewOnce &&
                attachment.localUri != null &&
                (attachment.status == DownloadStatus.COMPLETED || attachment.status == DownloadStatus.UPLOADED)
    }
    if (hasDownloadedAttachment && onSaveToDownloads != null) {
        actions.add(
            DropdownMenuAction(
                icon = Icons.Rounded.SaveAlt,
                text = UiText.StringResource(R.string.save_to_downloads),
                onClick = onSaveToDownloads
            )
        )
    }

    actions.addAll(item.dropdownActions)
    return actions
}
