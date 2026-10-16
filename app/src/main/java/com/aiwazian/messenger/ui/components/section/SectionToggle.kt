/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components.section

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SectionToggle(
    headlineText: String,
    isChecked: Boolean,
    onCheckedChange: () -> Unit,
    supportingText: String? = null,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val leading: (@Composable () -> Unit)? = if (leadingIcon != null) {
        {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        null
    }

    SectionItem(
        headlineContent = {
            Text(
                text = headlineText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )
        },
        supportingText = supportingText,
        leadingIcon = leading,
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onClick != null) {
                    VerticalDivider(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .padding(end = 10.dp)
                            .height(32.dp)
                    )
                }

                Switch(
                    enabled = enabled,
                    checked = isChecked,
                    onCheckedChange = if (onClick != null) {
                        { if (enabled) onCheckedChange() }
                    } else {
                        null
                    }
                )
            }
        },
        enabled = enabled,
        onClick = { if (enabled) (onClick ?: onCheckedChange)() }
    )
}
