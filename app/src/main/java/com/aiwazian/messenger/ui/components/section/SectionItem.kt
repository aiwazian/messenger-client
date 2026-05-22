/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components.section

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiwazian.messenger.utils.RegexPatterns

@Composable
fun SectionItem(
    headlineContent: @Composable () -> Unit,
    supportingText: String? = null,
    leadingIcon: ImageVector? = null,
    trailingText: String? = null,
    trailingContent: @Composable (() -> Unit) = {},
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    TextButton(
        shape = RectangleShape,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            ),
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            contentColor = contentColor
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    headlineContent()
                    
                    if (supportingText != null) {
                        Text(
                            text = supportingText,
                            fontSize = 12.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                if (trailingText != null) {
                    Text(
                        text = trailingText,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
                trailingContent()
            }
        }
    }
}

@Composable
fun SectionItem(
    headlineText: String,
    supportingText: String? = null,
    leadingIcon: ImageVector? = null,
    trailingText: String? = null,
    trailingContent: @Composable (() -> Unit) = {},
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    onLinkClicked: ((String) -> Unit)? = null,
    onUsernameClicked: ((String) -> Unit)? = null,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
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
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )
        },
        supportingText = supportingText,
        leadingIcon = leadingIcon,
        trailingText = trailingText,
        trailingContent = trailingContent,
        contentColor = contentColor,
        onClick = onClick,
        onLongClick = onLongClick
    )
}