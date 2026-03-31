/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

@Composable
fun AnimatedDotsText(text: String, dots: Int = 3) {
    var dotsCount by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            dotsCount = (dotsCount + 1) % 4
        }
    }
    
    Row {
        Text(text)
        repeat(dots) {
            AnimatedVisibility(
                visible = it < dotsCount,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(".")
            }
        }
    }
}