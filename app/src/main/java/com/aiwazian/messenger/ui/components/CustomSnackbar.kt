/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CustomSnackbar(text: String, onDismiss: (() -> Unit)? = null) {
    SwipeToDismissBox(
        state = rememberSwipeToDismissBoxState(),
        backgroundContent = {},
        onDismiss = { onDismiss?.invoke() }) {
        Row(
            modifier = Modifier
                .padding(10.dp)
                .clip(shape = MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Text(
                text = text,
                fontSize = 14.sp,
                lineHeight = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(10.dp),
            )
        }
    }
}
