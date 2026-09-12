/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiwazian.messenger.ui.components.rememberCustomEmojiInlineContent
import com.aiwazian.messenger.utils.RegexPatterns

@Composable
fun MessageText(
    text: String,
    onLinkClicked: ((String) -> Unit)? = null,
    onUsernameClicked: ((String) -> Unit)? = null,
    onEmailClicked: ((String) -> Unit)? = null
) {
    val parts = remember(text) { CustomEmojiText.parse(text) }
    
    val inlineContent = rememberCustomEmojiInlineContent(
        text = text,
        emojiSize = CUSTOM_EMOJI_SIZE
    )
    
    val linkColor = MaterialTheme.colorScheme.primary
    val pressedColor = MaterialTheme.colorScheme.primary.copy(alpha = LINK_PRESSED_ALPHA)
    
    val annotatedString = buildAnnotatedString {
        parts.forEach { part ->
            when (part) {
                is CustomEmojiTextPart.Text -> appendMessageText(
                    text = part.value,
                    linkColor = linkColor,
                    pressedColor = pressedColor,
                    onLinkClicked = onLinkClicked,
                    onUsernameClicked = onUsernameClicked,
                    onEmailClicked = onEmailClicked
                )
                
                is CustomEmojiTextPart.Emoji -> appendInlineContent(
                    id = part.emojiId.toString(),
                    alternateText = CustomEmojiText.PLACEHOLDER
                )
            }
        }
    }
    
    Text(
        text = annotatedString,
        fontSize = 16.sp,
        lineHeight = 18.sp,
        inlineContent = inlineContent,
        modifier = Modifier.padding(8.dp)
    )
}

private fun AnnotatedString.Builder.appendMessageText(
    text: String,
    linkColor: Color,
    pressedColor: Color,
    onLinkClicked: ((String) -> Unit)?,
    onUsernameClicked: ((String) -> Unit)?,
    onEmailClicked: ((String) -> Unit)?
) {
    var lastIndex = 0
    
    val emailMatches = RegexPatterns.EMAIL_IN_TEXT.findAll(text).toList()
    val emailRanges = emailMatches.map { it.range }
    
    val urlMatches = RegexPatterns.URL.findAll(text)
        .filterNot { url -> emailRanges.any { url.range.first <= it.last && it.first <= url.range.last } }
        .map { it to "url" }
    val usernameMatches = RegexPatterns.MENTION.findAll(text).map { it to "username" }
    
    val allMatches = (emailMatches.map { it to "email" } + urlMatches + usernameMatches)
        .sortedBy { it.first.range.first }
        .toList()
    
    if (allMatches.isEmpty()) {
        append(text)
        
        return
    }
    
    allMatches.forEach { (matchResult, type) ->
        val startIndex = matchResult.range.first
        val endIndex = matchResult.range.last + 1
        
        if (startIndex < lastIndex) return@forEach
        
        if (startIndex > lastIndex) {
            append(text.substring(lastIndex, startIndex))
        }
        
        val matchedValue = matchResult.value
        
        when (type) {
            "url" if onLinkClicked != null -> {
                withLink(
                    link = LinkAnnotation.Clickable(
                        tag = matchedValue,
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline
                            ),
                            pressedStyle = SpanStyle(background = pressedColor)
                        ),
                        linkInteractionListener = {
                            onLinkClicked(matchedValue)
                        }
                    )
                ) {
                    append(matchedValue)
                }
            }
            
            "email" if onEmailClicked != null -> {
                withLink(
                    link = LinkAnnotation.Clickable(
                        tag = matchedValue,
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline
                            ),
                            pressedStyle = SpanStyle(background = pressedColor)
                        ),
                        linkInteractionListener = {
                            onEmailClicked(matchedValue)
                        }
                    )
                ) {
                    append(matchedValue)
                }
            }
            
            "username" if onUsernameClicked != null -> {
                withLink(
                    link = LinkAnnotation.Clickable(
                        tag = matchedValue,
                        styles = TextLinkStyles(
                            style = SpanStyle(color = linkColor),
                            pressedStyle = SpanStyle(background = pressedColor)
                        ),
                        linkInteractionListener = {
                            onUsernameClicked(matchedValue)
                        }
                    )
                ) {
                    append(matchedValue)
                }
            }
            
            else -> {
                val style = if (type == "url" || type == "email") {
                    SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline
                    )
                } else {
                    SpanStyle(color = linkColor)
                }
                
                withStyle(style = style) {
                    append(matchedValue)
                }
            }
        }
        
        lastIndex = endIndex
    }
    
    if (lastIndex < text.length) {
        append(text.substring(lastIndex))
    }
}

private val CUSTOM_EMOJI_SIZE = 18.sp
private const val LINK_PRESSED_ALPHA = 0.4f
