/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp

private val TEXT_BOX_MIN_HEIGHT = 56.dp
private val TEXT_BOX_PADDING = 16.dp

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
    state: TextFieldState,
    modifier: Modifier = Modifier,
    inputTransformation: InputTransformation? = null,
    textStyle: TextStyle = LocalTextStyle.current,
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        capitalization = KeyboardCapitalization.Sentences
    ),
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.SingleLine
) {
    val resolvedTextStyle = if (textStyle.color == Color.Unspecified) {
        textStyle.copy(color = MaterialTheme.colorScheme.onSurface)
    } else {
        textStyle
    }
    
    BasicTextField(
        state = state,
        modifier = modifier.fillMaxWidth(),
        inputTransformation = inputTransformation,
        textStyle = resolvedTextStyle,
        keyboardOptions = keyboardOptions,
        lineLimits = lineLimits,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorator = TextFieldDecorator { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = TEXT_BOX_MIN_HEIGHT)
                    .padding(TEXT_BOX_PADDING),
                contentAlignment = Alignment.CenterStart
            ) {
                if (state.text.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = resolvedTextStyle
                    )
                }
                
                innerTextField()
            }
        })
}

@Composable
private fun framelessColors(): TextFieldColors = TextFieldDefaults.colors(
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
)
