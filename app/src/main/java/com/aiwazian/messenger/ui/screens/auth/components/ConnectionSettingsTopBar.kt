/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.auth.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.app.AppDropdownMenu
import com.aiwazian.messenger.ui.app.AppDropdownMenuItem
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack

@Composable
fun ConnectionSettingsTopBar() {
    val navBackStack = LocalNavBackStack.current
    var expanded by remember { mutableStateOf(false) }
    
    TopAppBar(
        title = {},
        actions = {
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = stringResource(R.string.actions)
                    )
                }
                
                AppDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    AppDropdownMenuItem(
                        text = stringResource(R.string.proxy_settings),
                        onClick = {
                            expanded = false
                            navBackStack.add(AppRoute.SettingsProxy)
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Rounded.Public, contentDescription = null)
                        }
                    )
                    AppDropdownMenuItem(
                        text = stringResource(R.string.dns),
                        onClick = {
                            expanded = false
                            navBackStack.add(AppRoute.SettingsDns)
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Rounded.Dns, contentDescription = null)
                        }
                    )
                }
            }
        }
    )
}
