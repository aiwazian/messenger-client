/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

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
import com.aiwazian.messenger.utils.RegexPatterns

private val URL_REGEX = Regex("(https?://)?([\\w-]+\\.)+[\\w-]+(:\\d+)?(/\\S*)?")
private val USERNAME_REGEX = Regex("^@[a-zA-Z0-9_]{5,32}$")

@Composable
fun MessageText(
    text: String,
    onLinkClicked: ((String) -> Unit)? = null,
    onUsernameClicked: ((String) -> Unit)? = null
) {
    val annotatedString = buildAnnotatedString {
        var lastIndex = 0
        
        val urlMatches = URL_REGEX.findAll(text).map { it to "url" }
        val usernameMatches = USERNAME_REGEX.findAll(text).map { it to "username" }
        
        val allMatches = (urlMatches + usernameMatches)
            .sortedBy { it.first.range.first }
            .toList()
        
        if (allMatches.isEmpty()) {
            append(text)
            return@buildAnnotatedString
        }
        
        allMatches.forEach { (matchResult, type) ->
            val startIndex = matchResult.range.first
            val endIndex = matchResult.range.last + 1
            
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
                                    color = MaterialTheme.colorScheme.primary,
                                    textDecoration = TextDecoration.Underline
                                ),
                                pressedStyle = SpanStyle(
                                    background = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                )
                            ),
                            linkInteractionListener = {
                                onLinkClicked(matchedValue)
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
                                style = SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                ),
                                pressedStyle = SpanStyle(
                                    background = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                )
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
                    val style = if (type == "url") {
                        SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    } else {
                        SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                        )
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
    
    Text(
        text = annotatedString,
        fontSize = 16.sp,
        lineHeight = 16.sp,
        modifier = Modifier.padding(8.dp)
    )
}
