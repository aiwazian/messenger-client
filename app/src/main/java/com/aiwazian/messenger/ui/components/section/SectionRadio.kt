/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components.section

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun SectionRadio(
    headlineText: String,
    selected: Boolean,
    supportingText: String? = null,
    trailingText: String? = null,
    radioColor: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
) {
    SectionItem(
        headlineContent = {
            Text(
                text = headlineText,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )
        },
        supportingText = supportingText,
        leadingIcon = {
            RadioButton(
                onClick = null,
                selected = selected,
                colors = RadioButtonDefaults.colors(
                    selectedColor = radioColor,
                    unselectedColor = radioColor
                )
            )
        },
        trailingText = trailingText,
        onClick = onClick
    )
}
