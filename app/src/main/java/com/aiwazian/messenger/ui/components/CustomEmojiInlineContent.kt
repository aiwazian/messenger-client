/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.ui.screens.chat.components.CustomEmojiText
import com.aiwazian.messenger.ui.screens.chat.components.CustomEmojiTextPart
import com.aiwazian.messenger.utils.MessageFormatSpan
import com.aiwazian.messenger.utils.MessageTextFormatter
import com.aiwazian.messenger.utils.MessageTextStyle

@Composable
fun rememberCustomEmojiInlineContent(
    text: String,
    emojiSize: TextUnit
): Map<String, InlineTextContent> {
    val customEmojiViewModel: CustomEmojiViewModel = hiltViewModel()
    val emojis by customEmojiViewModel.emojis.collectAsState()
    
    val emojiIds = remember(text) { customEmojiIds(text) }
    
    LaunchedEffect(emojiIds) {
        if (emojiIds.isNotEmpty()) {
            customEmojiViewModel.requestEmojis(emojiIds)
        }
    }
    
    if (emojiIds.isEmpty()) {
        return emptyMap()
    }
    
    return emojiIds.mapNotNull { emojiId ->
        val emoji = emojis[emojiId] ?: return@mapNotNull null
        
        emojiId.toString() to InlineTextContent(
            placeholder = Placeholder(
                width = emojiSize,
                height = emojiSize,
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
            )
        ) {
            AnimatedStickerImage(
                data = emoji.url,
                isVideo = isVideoMediaUrl(emoji.url),
                cacheKey = emoji.fileId,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
    }.toMap()
}

fun AnnotatedString.Builder.appendCustomEmojiText(text: String) {
    CustomEmojiText.parse(text).forEach { part ->
        when (part) {
            is CustomEmojiTextPart.Text -> append(part.value)

            is CustomEmojiTextPart.Emoji -> appendInlineContent(
                id = part.emojiId.toString(),
                alternateText = CustomEmojiText.PLACEHOLDER
            )
        }
    }
}

fun AnnotatedString.Builder.appendFormattedCustomEmojiText(text: String) {
    val format = MessageTextFormatter.parse(text)

    var plainIndex = 0

    CustomEmojiText.parse(format.plain).forEach { part ->
        when (part) {
            is CustomEmojiTextPart.Text -> {
                appendFormattedMessageRange(
                    text = format.plain,
                    from = plainIndex,
                    to = plainIndex + part.value.length,
                    formatSpans = format.spans
                )

                plainIndex += part.value.length
            }

            is CustomEmojiTextPart.Emoji -> {
                appendInlineContent(
                    id = part.emojiId.toString(),
                    alternateText = CustomEmojiText.PLACEHOLDER
                )

                plainIndex += CustomEmojiText.token(part.packId, part.emojiId).length
            }
        }
    }
}

internal fun AnnotatedString.Builder.appendFormattedMessageRange(
    text: String,
    from: Int,
    to: Int,
    formatSpans: List<MessageFormatSpan>
) {
    var index = from

    while (index < to) {
        var next = to

        formatSpans.forEach { span ->
            if (span.start > index && span.start < next) next = span.start
            if (span.end > index && span.end < next) next = span.end
        }

        val style = mergedMessageFormatStyle(formatSpans, index)

        if (style == null) {
            append(text.substring(index, next))
        } else {
            withStyle(style) { append(text.substring(index, next)) }
        }

        index = next
    }
}

internal fun mergedMessageFormatStyle(
    formatSpans: List<MessageFormatSpan>,
    index: Int
): SpanStyle? {
    var bold = false
    var italic = false
    var underline = false
    var strikethrough = false
    var monospace = false

    formatSpans.forEach { span ->
        if (span.start <= index && span.end > index) {
            when (span.style) {
                MessageTextStyle.Bold -> bold = true
                MessageTextStyle.Italic -> italic = true
                MessageTextStyle.Underline -> underline = true
                MessageTextStyle.Strikethrough -> strikethrough = true
                MessageTextStyle.Mono -> monospace = true
            }
        }
    }

    if (!bold && !italic && !underline && !strikethrough && !monospace) {
        return null
    }

    val decorations = buildList {
        if (underline) add(TextDecoration.Underline)
        if (strikethrough) add(TextDecoration.LineThrough)
    }

    return SpanStyle(
        fontWeight = if (bold) FontWeight.Bold else null,
        fontStyle = if (italic) FontStyle.Italic else null,
        fontFamily = if (monospace) FontFamily.Monospace else null,
        textDecoration = if (decorations.isEmpty()) null else TextDecoration.combine(decorations)
    )
}

private fun customEmojiIds(text: String): List<Long> = CustomEmojiText.parse(text)
    .filterIsInstance<CustomEmojiTextPart.Emoji>()
    .map { it.emojiId }
    .distinct()
