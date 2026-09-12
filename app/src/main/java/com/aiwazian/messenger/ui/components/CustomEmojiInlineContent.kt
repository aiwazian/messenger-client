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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.TextUnit
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.aiwazian.messenger.ui.screens.chat.components.CustomEmojiText
import com.aiwazian.messenger.ui.screens.chat.components.CustomEmojiTextPart

@Composable
fun rememberCustomEmojiInlineContent(
    text: String,
    emojiSize: TextUnit
): Map<String, InlineTextContent> {
    val context = LocalContext.current
    
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
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(emoji.url)
                    .memoryCacheKey(emoji.fileId)
                    .diskCacheKey(emoji.fileId)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
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

private fun customEmojiIds(text: String): List<Long> =
    CustomEmojiText.parse(text)
        .filterIsInstance<CustomEmojiTextPart.Emoji>()
        .map { it.emojiId }
        .distinct()
