/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components.section

import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun SectionCheckbox(
    headlineText: String,
    checked: Boolean,
    supportingText: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingText: String? = null,
    checkboxColor: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
) {
    SectionItem(
        headlineContent = {
            Text(
                text = headlineText,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
            )
        },
        supportingText = supportingText,
        leadingIcon = leadingIcon,
        trailingIcon = {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = checkboxColor,
                ),
            )
        },
        trailingText = trailingText,
        onClick = onClick,
    )
}
