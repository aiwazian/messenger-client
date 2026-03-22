/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AnnotatedTextMessage(text: String) {
    val annotatedString = buildAnnotatedString {
        val parts = text.split(" ")
        for (part in parts) {
            if (part.startsWith("@")) {
                pushStringAnnotation(
                    tag = "user",
                    annotation = part
                )
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) { append(part) }
                pop()
            } else {
                append(part)
            }
            append(" ")
        }
    }
    
    Text(
        text = annotatedString,
        modifier = Modifier.Companion
            .padding(
                start = 8.dp,
                end = 40.dp,
                bottom = 6.dp
            )
            .clip(RoundedCornerShape(4.dp)),
        lineHeight = 18.sp
    )
}