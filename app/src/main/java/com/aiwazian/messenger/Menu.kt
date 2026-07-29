/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuGroupShapes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aiwazian.messenger.ui.app.AppDropdownMenu

@Preview
@Composable
fun GE() {
    Column(Modifier.width(300.dp)) {
        var exp by remember { mutableStateOf(false) }
        Button(onClick = { exp = !exp }) {
            Text("b")
            AppDropdownMenu(
                onDismissRequest = { exp = false },
                expanded = exp
            ) {
                DropdownMenuGroup(
                    shapes = MenuGroupShapes(
                        MaterialTheme.shapes.medium.copy(bottomEnd = MaterialTheme.shapes.small.bottomEnd),
                        MaterialTheme.shapes.medium
                    ),
                    contentPadding = PaddingValues(2.dp)
                ) {
                    DropdownMenuItem(text = {
                        Text("dsfewfewwf")
                    }, onClick = {}, modifier = Modifier.clip(MaterialTheme.shapes.medium))
                }
                Spacer(Modifier.height(2.dp))
                DropdownMenuGroup(
                    shapes = MenuGroupShapes(
                        MaterialTheme.shapes.medium, MaterialTheme.shapes.medium
                    )
                ) {
                    DropdownMenuItem(text = {
                        Text("dsfewfewwf")
                    }, onClick = {})
                }
            }
        }
        
        var exp2 by remember { mutableStateOf(false) }
        Button(onClick = { exp2 = !exp2 }) {
            Text("b")
            DropdownMenu(expanded = exp2, onDismissRequest = { exp2 = false }) {
                DropdownMenuItem(text = {
                    Text("dsfewfewwf", color = Color.Green)
                }, onClick = {})
            }
        }
    }
}
