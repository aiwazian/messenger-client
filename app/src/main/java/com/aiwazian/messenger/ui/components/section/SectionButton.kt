/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components.section

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.sp
import com.aiwazian.messenger.utils.LinkNormalizer
import com.aiwazian.messenger.utils.RegexPatterns

@Composable
fun SectionButton(
    headlineText: String,
    supportingText: String? = null,
    leadingIcon: ImageVector? = null,
    trailingText: String? = null,
    trailingContent: @Composable () -> Unit = {},
    contentColor: Color = Color.Unspecified,
    enabled: Boolean = true,
    onLinkClicked: ((String) -> Unit)? = null,
    onUsernameClicked: ((String) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val leading: (@Composable () -> Unit)? = if (leadingIcon != null) {
        {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = if (contentColor.isUnspecified) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    contentColor
                }
            )
        }
    } else {
        null
    }
    
    SectionItem(
        headlineContent = {
            val annotatedString = buildAnnotatedString {
                var lastIndex = 0
                
                val urlMatches = RegexPatterns.URL.findAll(headlineText).map { it to "url" }
                val usernameMatches =
                    RegexPatterns.MENTION.findAll(headlineText).map { it to "username" }
                
                val allMatches = (urlMatches + usernameMatches)
                    .sortedBy { it.first.range.first }
                    .toList()
                
                if (allMatches.isEmpty()) {
                    append(headlineText)
                } else {
                    allMatches.forEach { (matchResult, type) ->
                        val startIndex = matchResult.range.first
                        val endIndex = matchResult.range.last + 1
                        
                        if (startIndex < lastIndex) return@forEach
                        
                        if (startIndex > lastIndex) {
                            append(headlineText.substring(lastIndex, startIndex))
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
                                                background = MaterialTheme.colorScheme.primary.copy(
                                                    alpha = 0.4f
                                                )
                                            )
                                        ),
                                        linkInteractionListener = {
                                            onLinkClicked(LinkNormalizer.normalize(matchedValue))
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
                                                background = MaterialTheme.colorScheme.primary.copy(
                                                    alpha = 0.4f
                                                )
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
                                append(matchedValue)
                            }
                        }
                        
                        lastIndex = endIndex
                    }
                    
                    if (lastIndex < headlineText.length) {
                        append(headlineText.substring(lastIndex))
                    }
                }
            }
            
            Text(
                text = annotatedString,
                color = if (contentColor.isUnspecified) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    contentColor
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )
        },
        supportingText = supportingText,
        leadingIcon = leading,
        trailingIcon = trailingContent,
        trailingText = trailingText,
        contentColor = contentColor,
        enabled = enabled,
        onClick = onClick,
        onLongClick = onLongClick
    )
}
