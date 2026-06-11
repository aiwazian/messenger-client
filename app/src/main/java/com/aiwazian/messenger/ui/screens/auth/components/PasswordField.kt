/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.auth.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun PasswordField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean,
    supportingText: String?,
    onSendClick: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }
    
    InputTextField(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        label = label,
        isError = isError,
        visualTransformation = if (!passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = {
            IconButton(
                onClick = {
                    passwordVisible = !passwordVisible
                },
                modifier = Modifier.padding(end = 2.dp)
            ) {
                AnimatedContent(
                    targetState = passwordVisible,
                    transitionSpec = { fadeIn() togetherWith fadeOut() }) { isVisible ->
                    if (isVisible) {
                        Icon(
                            imageVector = Icons.Outlined.Visibility,
                            contentDescription = null,
                            tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.VisibilityOff,
                            contentDescription = null,
                            tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        supportingText = supportingText,
        onSendClick = onSendClick
    )
}
