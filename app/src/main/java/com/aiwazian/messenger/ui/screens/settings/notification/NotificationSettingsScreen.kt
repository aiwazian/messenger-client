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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionDescription
import com.aiwazian.messenger.ui.components.section.SectionToggleItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar

@Composable
fun NotificationSettingsScreen(viewModel: NotificationSettingsViewModel = hiltViewModel()) {
    val navBackStack = LocalNavBackStack.current
    
    val settings by viewModel.uiState.collectAsState()
    
    /*
     * Локальный кэш показывается сразу, но настройку могли поменять на другом
     * устройстве, пока это лежало без сети — сверяемся с сервером при открытии.
     */
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }
    
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
            SectionContainer(footer = {
                SectionDescription(text = stringResource(R.string.notifications_description))
            }) {
                SectionToggleItem(
                    text = stringResource(R.string.private_chats),
                    isChecked = settings.privateChats,
                    onCheckedChange = viewModel::togglePrivateChats)
                SectionToggleItem(
                    text = stringResource(R.string.groups),
                    isChecked = settings.groups,
                    onCheckedChange = viewModel::toggleGroups)
                SectionToggleItem(
                    text = stringResource(R.string.channels),
                    isChecked = settings.channels,
                    onCheckedChange = viewModel::toggleChannels)
            }
        }
    }
}
