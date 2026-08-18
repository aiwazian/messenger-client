/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SectionToggleItem(
    text: String,
    supportingText: String? = null,
    isChecked: Boolean,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onCheckedChange: () -> Unit,
) {
    TextButton(
        shape = RectangleShape,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        /*
         * Без onClick вся строка — это переключатель. Есть onClick — по строке
         * открывается свой экран, а переключение остаётся только за самим Switch.
         */
        onClick = { if (enabled) (onClick ?: onCheckedChange)() },
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 8.dp, end = 4.dp, bottom = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
            ) {
                Text(
                    text = text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal
                )
                if (!supportingText.isNullOrBlank()) {
                    Text(
                        text = supportingText,
                        color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 12.sp
                    )
                }
            }
            
            if (onClick != null) {
                /*
                 * Строка ведёт на другой экран, поэтому отделяем тумблер чертой и
                 * даём ему собственное нажатие: тап по Switch переключает и не
                 * открывает экран, а тап по строке экран открывает и Switch не трогает.
                 */
                VerticalDivider(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .height(32.dp)
                )
                Switch(
                    enabled = enabled,
                    checked = isChecked,
                    onCheckedChange = { if (enabled) onCheckedChange() },
                )
            } else {
                Box(Modifier.pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent()
                        }
                    }
                }) {
                    Switch(
                        enabled = enabled,
                        checked = isChecked,
                        onCheckedChange = null,
                    )
                }
            }
        }
    }
}
