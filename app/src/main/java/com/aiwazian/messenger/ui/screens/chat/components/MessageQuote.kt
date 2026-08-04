/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults.rememberTooltipPositionProvider
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

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

@Composable
fun ReplyQuote(
    preview: MessageReplyPreview,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        targetValue = if (isPressed) 0.98f else 1f,
        label = "reply_quote_scale_animation"
    )
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .height(IntrinsicSize.Min)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .then(
                if (onClick != null) Modifier.combinedClickable(
                    interactionSource = interactionSource,
                    onClick = onClick,
                    onLongClick = onClick
                )
                else Modifier
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(3.dp)
                .background(accentColor)
        )
        
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = preview.title.orEmpty(),
                fontSize = 14.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = replyPreviewText(preview),
                fontSize = 14.sp,
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
    val tooltipState = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    
    val restrictedRes = when (ChatType.fromId(forwardedFrom.chatId)) {
        ChatType.CHANNEL -> R.string.forwarded_from_private_channel
        ChatType.GROUP -> R.string.forwarded_from_private_group
        else -> R.string.forwarded_from_hidden_account
    }
    
    LaunchedEffect(tooltipState.isVisible) {
        if (tooltipState.isVisible) {
            delay(3.seconds)
            tooltipState.dismiss()
        }
    }
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        targetValue = if (isPressed) 0.96f else 1f,
        label = "forward_scale_animation"
    )
    
    TooltipBox(
        positionProvider = rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = {
            PlainTooltip(
                modifier = Modifier.padding(horizontal = 8.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(stringResource(restrictedRes), lineHeight = 12.sp)
            }
        },
        state = tooltipState,
        enableUserInput = false
    ) {
        Column(
            modifier = modifier
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .clip(MaterialTheme.shapes.extraSmall)
                .clickable(interactionSource = interactionSource) {
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
