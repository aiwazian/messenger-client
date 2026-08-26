/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components.topBar

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.aiwazian.messenger.ui.app.AppDropdownMenu
import com.aiwazian.messenger.ui.app.AppDropdownMenuItem

/**
 * @param subtitle пояснение под заголовком: чего и сколько на экране.
 *
 * Размер и цвет задаются здесь, а не на стороне вызывающего: иначе каждый
 * экран повторял бы оформление руками и шапки разъехались бы по приложению.
 */
@Composable
fun PageTopBar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit = { },
    navigationIcon: NavigationIcon,
    actions: List<TopBarAction> = emptyList(),
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    subtitle: (@Composable () -> Unit)? = null
) {
    TopAppBar(
        title = {
            if (subtitle == null) {
                title()
            } else {
                Column {
                    title()
                    
                    ProvideTextStyle(MaterialTheme.typography.labelMedium) {
                        CompositionLocalProvider(
                            LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            subtitle()
                        }
                    }
                }
            }
        },
        modifier = modifier,
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
                    expand = action.dropdownActions.isNotEmpty()
                }) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = null
                    )
                }
                
                AppDropdownMenu(
                    expanded = expand,
                    onDismissRequest = { expand = false }) {
                    action.dropdownActions.forEach { item ->
                        AppDropdownMenuItem(
                            text = item.text.asString(),
                            onClick = {
                                expand = false
                                item.onClick?.invoke()
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
        },
        colors = colors
    )
}
