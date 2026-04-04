/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components.topBar

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.aiwazian.messenger.ui.components.DropdownMenu

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageTopBar(
    title: @Composable () -> Unit = { },
    navigationIcon: NavigationIcon,
    actions: List<TopBarAction> = emptyList()
) {
    TopAppBar(
        title = title,
        navigationIcon = {
            IconButton(onClick = { navigationIcon.onClick() }) {
                Icon(
                    imageVector = navigationIcon.icon,
                    contentDescription = null
                )
            }
        },
        actions = {
            actions.forEach { action ->
                var expand by remember { mutableStateOf(false) }
                
                IconButton(onClick = {
                    action.onClick?.invoke()
                    
                    if (action.dropdownActions.isNotEmpty()) {
                        expand = true
                    }
                }) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = null
                    )
                }
                
                DropdownMenu(
                    expanded = expand,
                    onDismissRequest = { expand = false }) {
                    action.dropdownActions.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(stringResource(item.textResId)) },
                            onClick = {
                                expand = false
                                item.onClick()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null
                                )
                            })
                    }
                }
            }
        })
}
