/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

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
import androidx.compose.ui.unit.sp
import com.aiwazian.messenger.ui.components.rememberCustomEmojiInlineContent
import com.aiwazian.messenger.utils.LinkNormalizer
import com.aiwazian.messenger.utils.PhoneLinks
import com.aiwazian.messenger.utils.RegexPatterns

@Composable
fun MessageText(
    text: String,
    modifier: Modifier = Modifier,
    onLinkClicked: ((String) -> Unit)? = null,
    onUsernameClicked: ((String) -> Unit)? = null,
    onEmailClicked: ((String) -> Unit)? = null,
    onPhoneClicked: ((String) -> Unit)? = null
) {
    val parts = remember(text) { CustomEmojiText.parse(text) }

    val inlineContent = rememberCustomEmojiInlineContent(
        text = text,
        emojiSize = CUSTOM_EMOJI_SIZE
    )

    val linkColor = MaterialTheme.colorScheme.primary
    val pressedColor = MaterialTheme.colorScheme.primary.copy(alpha = LINK_PRESSED_ALPHA)
    val deviceRegion = remember { PhoneLinks.currentDeviceRegion() }

    val annotatedString = buildAnnotatedString {
        parts.forEach { part ->
            when (part) {
                is CustomEmojiTextPart.Text -> appendMessageText(
                    text = part.value,
                    region = deviceRegion,
                    linkColor = linkColor,
                    pressedColor = pressedColor,
                    onLinkClicked = onLinkClicked,
                    onUsernameClicked = onUsernameClicked,
                    onEmailClicked = onEmailClicked,
                    onPhoneClicked = onPhoneClicked
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
        modifier = modifier
    )
}

private data class TextEntity(
    val start: Int,
    val end: Int,
    val value: String,
    val linkValue: String,
    val type: String
)

private fun AnnotatedString.Builder.appendMessageText(
    text: String,
    region: String,
    linkColor: Color,
    pressedColor: Color,
    onLinkClicked: ((String) -> Unit)?,
    onUsernameClicked: ((String) -> Unit)?,
    onEmailClicked: ((String) -> Unit)?,
    onPhoneClicked: ((String) -> Unit)?
) {
    var lastIndex = 0

    val emailMatches = RegexPatterns.EMAIL_IN_TEXT.findAll(text).toList()
    val emailRanges = emailMatches.map { it.range }

    val urlMatches = RegexPatterns.URL.findAll(text)
        .filterNot { url -> emailRanges.any { url.range.first <= it.last && it.first <= url.range.last } }
        .toList()
    val urlRanges = urlMatches.map { it.range }

    val mentionMatches = RegexPatterns.MENTION.findAll(text).toList()

    val phoneEntities = PhoneLinks.findInText(text, region)
        .filterNot { phone ->
            (emailRanges + urlRanges).any { range ->
                phone.range.first <= range.last && range.first <= phone.range.last
            }
        }
        .map { phone ->
            TextEntity(
                start = phone.range.first,
                end = phone.range.last + 1,
                value = text.substring(phone.range.first, phone.range.last + 1),
                linkValue = phone.e164,
                type = "phone"
            )
        }

    val emailEntities = emailMatches.map { match ->
        TextEntity(
            start = match.range.first,
            end = match.range.last + 1,
            value = match.value,
            linkValue = match.value,
            type = "email"
        )
    }

    val urlEntities = urlMatches.map { match ->
        TextEntity(
            start = match.range.first,
            end = match.range.last + 1,
            value = match.value,
            linkValue = LinkNormalizer.normalize(match.value),
            type = "url"
        )
    }

    val mentionEntities = mentionMatches.map { match ->
        TextEntity(
            start = match.range.first,
            end = match.range.last + 1,
            value = match.value,
            linkValue = match.value,
            type = "username"
        )
    }

    val entities = (emailEntities + urlEntities + mentionEntities + phoneEntities)
        .sortedBy { it.start }

    if (entities.isEmpty()) {
        append(text)

        return
    }

    entities.forEach { entity ->
        if (entity.start < lastIndex) return@forEach

        if (entity.start > lastIndex) {
            append(text.substring(lastIndex, entity.start))
        }

        when (entity.type) {
            "url" if onLinkClicked != null -> {
                withLink(
                    link = LinkAnnotation.Clickable(
                        tag = entity.value,
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline
                            ),
                            pressedStyle = SpanStyle(background = pressedColor)
                        ),
                        linkInteractionListener = {
                            onLinkClicked(entity.linkValue)
                        }
                    )
                ) {
                    append(entity.value)
                }
            }

            "email" if onEmailClicked != null -> {
                withLink(
                    link = LinkAnnotation.Clickable(
                        tag = entity.value,
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline
                            ),
                            pressedStyle = SpanStyle(background = pressedColor)
                        ),
                        linkInteractionListener = {
                            onEmailClicked(entity.linkValue)
                        }
                    )
                ) {
                    append(entity.value)
                }
            }

            "phone" if onPhoneClicked != null -> {
                withLink(
                    link = LinkAnnotation.Clickable(
                        tag = entity.value,
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline
                            ),
                            pressedStyle = SpanStyle(background = pressedColor)
                        ),
                        linkInteractionListener = {
                            onPhoneClicked(entity.linkValue)
                        }
                    )
                ) {
                    append(entity.value)
                }
            }

            "username" if onUsernameClicked != null -> {
                withLink(
                    link = LinkAnnotation.Clickable(
                        tag = entity.value,
                        styles = TextLinkStyles(
                            style = SpanStyle(color = linkColor),
                            pressedStyle = SpanStyle(background = pressedColor)
                        ),
                        linkInteractionListener = {
                            onUsernameClicked(entity.linkValue)
                        }
                    )
                ) {
                    append(entity.value)
                }
            }

            else -> {
                val style = if (entity.type == "url" || entity.type == "email" || entity.type == "phone") {
                    SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline
                    )
                } else {
                    SpanStyle(color = linkColor)
                }

                withStyle(style = style) {
                    append(entity.value)
                }
            }
        }

        lastIndex = entity.end
    }

    if (lastIndex < text.length) {
        append(text.substring(lastIndex))
    }
}

private val CUSTOM_EMOJI_SIZE = 18.sp
private const val LINK_PRESSED_ALPHA = 0.4f
