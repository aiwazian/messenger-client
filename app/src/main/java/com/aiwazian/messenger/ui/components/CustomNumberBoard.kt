/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CustomNumberBoard(
    value: String = "",
    buttons: List<List<Any?>>,
    onChange: (String) -> Unit,
    bottomRightIcon: ImageVector = Icons.AutoMirrored.Rounded.Backspace,
    onBottomRightClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        buttons.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    
                    if (key == null) {
                        Box(modifier = Modifier.weight(1f))
                        return@forEach
                    }
                    
                    NumberButton(
                        onClick = {
                            if (key is ImageVector) {
                                if (onBottomRightClick != null) {
                                    onBottomRightClick()
                                } else {
                                    onChange(value.dropLast(1))
                                }
                            } else {
                                onChange(value + key)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 2.dp)
                    ) {
                        if (key is ImageVector) {
                            Text(
                                text = "",
                                lineHeight = 30.sp
                            )
                            
                            AnimatedContent(
                                targetState = bottomRightIcon,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "icon_transition"
                            ) { icon ->
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                )
                            }
                        } else if (key is String) {
                            Text(
                                text = key,
                                fontSize = 18.sp,
                                lineHeight = 30.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
            containerColor = MaterialTheme.colorScheme.surface,
        )
    ) {
        content()
    }
}

