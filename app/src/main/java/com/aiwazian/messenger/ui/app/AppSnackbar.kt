/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppSnackbar(
    hostState: SnackbarHostState,
    leadingIcon: ImageVector? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    SnackbarHost(hostState = hostState) { data ->
        SwipeToDismissBox(
            state = rememberSwipeToDismissBoxState(),
            backgroundContent = {},
            modifier = Modifier.padding(10.dp),
            onDismiss = { data.dismiss() }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape = MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(10.dp))
                
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = data.visuals.message,
                    modifier = Modifier
                        .weight(1f)
                        .padding(10.dp),
                    fontSize = 14.sp,
                    lineHeight = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                if (trailingIcon != null) {
                    trailingIcon()
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }
    }
}
