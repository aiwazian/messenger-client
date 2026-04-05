/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val URL_REGEX = Regex("(?:https?://)?(?:aiwazian\\.ru/\\+[a-f0-9]+|[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z]{2,})(?:/[^\\s]*)?)")

@Composable
fun MessageText(
    text: String,
    onLinkClicked: ((String) -> Unit)? = null
) {
    Box(
        modifier = Modifier.padding(8.dp)
    ) {
        val annotatedString = buildAnnotatedString {
            var lastIndex = 0
            val matches = URL_REGEX.findAll(text).toList()
            
            if (matches.isEmpty()) {
                append(text)
                return@buildAnnotatedString
            }
            
            matches.forEach { matchResult ->
                val startIndex = matchResult.range.first
                val endIndex = matchResult.range.last + 1
                
                if (startIndex > lastIndex) {
                    append(text.substring(lastIndex, startIndex))
                }
                
                val matchedUrl = matchResult.value
                
                if (onLinkClicked != null) {
                    withLink(
                        link = LinkAnnotation.Clickable(
                            tag = matchedUrl,
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    textDecoration = TextDecoration.Underline
                                ),
                                pressedStyle = SpanStyle(
                                    background = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                )
                            ),
                            linkInteractionListener = {
                                onLinkClicked(matchedUrl)
                            }
                        )
                    ) {
                        append(matchedUrl)
                    }
                } else {
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append(matchedUrl)
                    }
                }
                
                lastIndex = endIndex
            }
            
            if (lastIndex < text.length) {
                append(text.substring(lastIndex))
            }
        }
        
        Text(
            text = annotatedString,
            fontSize = 16.sp,
            lineHeight = 16.sp
        )
    }
}
