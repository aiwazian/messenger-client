/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aiwazian.messenger.R

@Composable
fun MediaDrawControls(
    color: Color,
    isEraser: Boolean,
    onColorClick: () -> Unit,
    onEraserSelected: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MediaDrawToolsRow(
            color = color,
            isEraser = isEraser,
            onColorClick = onColorClick,
            onEraserSelected = onEraserSelected
        )
        
        MediaEditorActionButtons(
            onCancel = onCancel,
            onDone = onDone
        )
    }
}

@Composable
fun MediaEditorActionButtons(
    onCancel: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        MediaOverlayControl {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.padding(horizontal = 4.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(text = stringResource(R.string.cancel).uppercase())
            }
        }
        
        MediaOverlayControl {
            TextButton(
                onClick = onDone,
                modifier = Modifier.padding(horizontal = 4.dp),
            ) {
                Text(text = stringResource(R.string.done).uppercase())
            }
        }
    }
}

@Composable
internal fun MediaOverlayControl(
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier.background(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = CircleShape
        )
    ) {
        content()
    }
}
