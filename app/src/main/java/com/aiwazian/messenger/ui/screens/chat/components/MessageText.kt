/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MessageText(
    text: String,
) {
    Box(
        modifier = Modifier.padding(8.dp)
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            lineHeight = 16.sp
        )
    }
}
