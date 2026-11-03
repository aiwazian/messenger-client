/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import android.content.Context
import androidx.annotation.StringRes
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

internal fun formatStatusTime(context: Context, timestamp: Long, @StringRes todayVerbRes: Int): String {
    val instant = timestamp.toInstance()
    val date = instant.atZone(ZoneId.systemDefault())
    val today = LocalDate.now()
    val time = instant.toPrettyTime()
    val todayVerb = context.getString(todayVerbRes)

    return when (date.toLocalDate()) {
        today -> context.getString(R.string.status_time_today, todayVerb, time)
        today.minusDays(1) -> context.getString(R.string.status_time_yesterday, time)
        else -> context.getString(R.string.status_time_other, date.format(DateTimeFormatter.ofPattern("d MMMM")), time)
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
    context: Context,
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
                    formatStatusTime(context, item.message.editedAt, todayVerbRes = R.string.edited)
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
                            formatStatusTime(context, readAt, todayVerbRes = R.string.read_status)
                        ),
                        onClick = null
                    )
                )
            }
        } else if (item.chatType == ChatType.GROUP && !readInfo.isNullOrEmpty()) {
            val count = readInfo.size
            actions.add(
                DropdownMenuAction(
                    icon = Icons.Rounded.DoneAll,
                    text = UiText.PluralResource(R.plurals.views_count, count, count),
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
