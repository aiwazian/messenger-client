/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import kotlinx.coroutines.delay

@Composable
fun CountdownTextButton(
    text: String,
    seconds: Int,
    modifier: Modifier = Modifier,
    shape: Shape = ButtonDefaults.textShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    onClickWhileRunning: () -> Unit,
    onClickAfterFinish: () -> Unit
) {
    var waitSeconds by remember { mutableIntStateOf(seconds) }
    
    LaunchedEffect(Unit) {
        while (seconds > 0) {
            delay(1000)
            waitSeconds--
        }
    }
    
    TextButton(onClick = {
        if (waitSeconds > 0) {
            onClickWhileRunning()
        } else {
            onClickAfterFinish()
        }
    }, modifier = modifier, shape = shape, colors = colors) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = text)
            
            AnimatedContent(
                targetState = waitSeconds, transitionSpec = {
                    slideInVertically { it } + fadeIn() togetherWith slideOutVertically { -it } + fadeOut()
                }) { second ->
                if (second > 0) {
                    Text(
                        text = " $second",
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
