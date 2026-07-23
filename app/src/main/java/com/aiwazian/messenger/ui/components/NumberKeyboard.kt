/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aiwazian.messenger.utils.VibrationPattern

@Composable
fun NumberKeyboard(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxLength: Int = Int.MAX_VALUE,
    keySpacing: Dp = 8.dp,
    keyHeight: Dp = 56.dp,
    bottomLeft: (@Composable () -> Unit)? = null,
    rightIcon: ImageVector = Icons.AutoMirrored.Outlined.Backspace,
    onRightClick: (() -> Unit)? = null,
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
    )
    
    fun input(digit: String) {
        if (value.length < maxLength) onValueChange(value + digit)
    }
    
    Column(
        modifier = Modifier
            .padding(keySpacing)
            .then(modifier),
        verticalArrangement = Arrangement.spacedBy(keySpacing),
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(keySpacing)) {
                row.forEach { digit ->
                    DigitKey(
                        digit = digit,
                        modifier = Modifier
                            .weight(1f)
                            .height(keyHeight),
                        onClick = { input(digit) },
                    )
                }
            }
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(keySpacing)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(keyHeight),
                contentAlignment = Alignment.Center,
            ) {
                bottomLeft?.invoke()
            }
            
            DigitKey(
                digit = "0",
                modifier = Modifier
                    .weight(1f)
                    .height(keyHeight),
                onClick = { input("0") },
            )
            
            KeyButton(
                modifier = Modifier
                    .weight(1f)
                    .height(keyHeight),
                onClick = {
                    if (onRightClick != null) {
                        onRightClick()
                    } else if (value.isNotEmpty()) {
                        onValueChange(value.dropLast(1))
                    }
                },
            ) {
                Icon(
                    imageVector = rightIcon,
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
private fun DigitKey(
    digit: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    KeyButton(modifier = modifier, onClick = onClick) {
        Text(
            text = digit,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Кнопка-ячейка клавиатуры: серый скруглённый прямоугольник во всю ячейку. */
@Composable
private fun KeyButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    TextButton(
        onClick = {
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    VibrationPattern.TactileResponse,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
            onClick()
        },
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}
