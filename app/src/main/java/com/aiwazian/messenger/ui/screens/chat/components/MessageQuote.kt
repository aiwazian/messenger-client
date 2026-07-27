/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.ForwardedFrom
import com.aiwazian.messenger.domain.MessageReplyPreview
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.enums.ForwardSourceAccess
import kotlinx.coroutines.launch

/**
 * Текст превью цитаты: если текста нет, показываем тип вложения:
 * «Фото», «Видео», «Голосовое сообщение», «Файл».
 */
@Composable
fun replyPreviewText(preview: MessageReplyPreview): String {
    val text = preview.text
    if (!text.isNullOrBlank()) return text
    
    val type = preview.attachmentTypes.firstOrNull() ?: return ""
    return stringResource(
        when (type) {
            AttachmentType.IMAGE -> R.string.attachment_photo
            AttachmentType.GIF -> R.string.attachment_gif
            AttachmentType.VIDEO -> R.string.attachment_video
            AttachmentType.VOICE -> R.string.voice_message
            AttachmentType.FILE -> R.string.attachment_file
        }
    )
}

/**
 * Цитата сообщения: вертикальная полоса, заголовок и одна строка текста.
 *
 * Используется и внутри сообщения, и в панели над полем ввода.
 */
@Composable
fun ReplyQuote(
    preview: MessageReplyPreview,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val accentColor = MaterialTheme.colorScheme.primary
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable { onClick() } else Modifier
        )
    ) {
        Column(
            modifier = Modifier
                .width(3.dp)
                .height(32.dp)
                .clip(CircleShape)
                .background(accentColor)
        ) {}
        
        Column(modifier = Modifier.padding(start = 6.dp)) {
            Text(
                text = preview.title.orEmpty(),
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = replyPreviewText(preview),
                fontSize = 12.sp,
                lineHeight = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Заголовок «Переслано от» с названием источника.
 *
 * OPEN — клик ведёт в чат, RESTRICTED — тултип про закрытый источник,
 * UNAVAILABLE — клик ничего не делает.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForwardedFromHeader(
    forwardedFrom: ForwardedFrom,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val tooltipState = rememberTooltipState()
    val scope = rememberCoroutineScope()
    
    val restrictedRes = when (ChatType.fromId(forwardedFrom.chatId)) {
        ChatType.CHANNEL -> R.string.forwarded_from_private_channel
        ChatType.GROUP -> R.string.forwarded_from_private_group
        else -> R.string.forwarded_from_hidden_account
    }
    
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(stringResource(restrictedRes)) } },
        state = tooltipState,
        enableUserInput = false
    ) {
        Column(
            modifier = modifier.clickable {
                when (forwardedFrom.access) {
                    ForwardSourceAccess.OPEN -> onClick()
                    ForwardSourceAccess.RESTRICTED -> scope.launch { tooltipState.show() }
                    ForwardSourceAccess.UNAVAILABLE -> Unit
                }
            }
        ) {
            Text(
                text = stringResource(R.string.forwarded_from),
                fontSize = 12.sp,
                lineHeight = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = forwardedFrom.name.ifBlank { stringResource(R.string.unknown_chat) },
                fontSize = 13.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
