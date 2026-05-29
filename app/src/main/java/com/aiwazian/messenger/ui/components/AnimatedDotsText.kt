/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun AnimatedDotsText(
    text: String,
    fontSize: TextUnit = TextUnit.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified,
    color: Color = Color.Unspecified
) {
    var dotsCount by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            dotsCount = (dotsCount + 1) % 4
        }
    }
    
    Row {
        Text(text = text, fontSize = fontSize, lineHeight = lineHeight, color = color)
        Row(modifier = Modifier.widthIn(min = 12.dp)) {
            repeat(3) {
                AnimatedVisibility(
                    visible = it < dotsCount,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = ".",
                        fontSize = fontSize,
                        lineHeight = lineHeight,
                        color = color,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.sp
                    )
                }
            }
        }
    }
}