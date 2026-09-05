/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue

@Composable
fun FramelessTextBox(
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    singleLine: Boolean = true,
    textStyle: TextStyle = LocalTextStyle.current,
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        capitalization = KeyboardCapitalization.Sentences
    ),
    trailingIcon: @Composable (() -> Unit)? = null
) {
    TextField(
        modifier = modifier.fillMaxWidth(),
        value = value,
        singleLine = singleLine,
        maxLines = maxLines,
        textStyle = textStyle,
        placeholder = { Text(placeholder) },
        onValueChange = onValueChange,
        trailingIcon = trailingIcon,
        keyboardOptions = keyboardOptions,
        colors = framelessColors()
    )
}

@Composable
fun FramelessTextBox(
    placeholder: String,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    singleLine: Boolean = true,
    textStyle: TextStyle = LocalTextStyle.current,
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        capitalization = KeyboardCapitalization.Sentences
    ),
    trailingIcon: @Composable (() -> Unit)? = null
) {
    TextField(
        modifier = modifier.fillMaxWidth(),
        value = value,
        singleLine = singleLine,
        maxLines = maxLines,
        textStyle = textStyle,
        placeholder = { Text(placeholder) },
        onValueChange = onValueChange,
        trailingIcon = trailingIcon,
        keyboardOptions = keyboardOptions,
        colors = framelessColors()
    )
}

@Composable
private fun framelessColors(): TextFieldColors = TextFieldDefaults.colors(
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
)
