/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.notification

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionToggleItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar

@Composable
fun NotificationSettingsScreen() {
    val navBackStack = LocalNavBackStack.current
    
    Scaffold(topBar = {
        PageTopBar(
            navigationIcon = NavigationIcon(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                onClick = navBackStack::removeLastOrNull
            ),
            title = {
                Text(stringResource(R.string.notifications))
            }
        )
    }) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            SectionContainer {
                SectionToggleItem(
                    text = stringResource(R.string.private_chats),
                    isChecked = false,
                    onCheckedChange = {})
                SectionToggleItem(
                    text = stringResource(R.string.groups),
                    isChecked = false,
                    onCheckedChange = {})
                SectionToggleItem(
                    text = stringResource(R.string.channels),
                    isChecked = false,
                    onCheckedChange = {})
            }
        }
    }
}
