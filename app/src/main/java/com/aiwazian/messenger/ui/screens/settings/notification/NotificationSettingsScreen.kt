/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.notification

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.ChatFolderCategory
import com.aiwazian.messenger.ui.app.AppDialog
import com.aiwazian.messenger.ui.app.AppScaffold
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.section.SectionToggleItem
import com.aiwazian.messenger.ui.components.topBar.PageTopBar

@Composable
fun NotificationSettingsScreen(viewModel: NotificationSettingsViewModel = hiltViewModel()) {
    val navBackStack = LocalNavBackStack.current
    
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }
    
    AppScaffold(topBar = {
        PageTopBar(
            title = {
                Text(stringResource(R.string.notifications))
            },
        )
    }) {
        SectionContainer {
            SectionToggleItem(
                text = stringResource(R.string.private_chats),
                supportingText = pluralStringResource(
                    R.plurals.notification_exceptions_count,
                    uiState.privateChatExceptions,
                    uiState.privateChatExceptions
                ),
                isChecked = uiState.settings.privateChats,
                leadingIcon = Icons.Outlined.Person,
                onClick = {
                    navBackStack.add(
                        AppRoute.SettingsNotificationCategory(ChatFolderCategory.PRIVATE_CHATS)
                    )
                },
                onCheckedChange = viewModel::togglePrivateChats
            )
            SectionToggleItem(
                text = stringResource(R.string.groups),
                supportingText = pluralStringResource(
                    R.plurals.notification_exceptions_count,
                    uiState.groupExceptions,
                    uiState.groupExceptions
                ),
                isChecked = uiState.settings.groups,
                leadingIcon = Icons.Outlined.Group,
                onClick = {
                    navBackStack.add(
                        AppRoute.SettingsNotificationCategory(ChatFolderCategory.GROUPS)
                    )
                },
                onCheckedChange = viewModel::toggleGroups
            )
            SectionToggleItem(
                text = stringResource(R.string.channels),
                supportingText = pluralStringResource(
                    R.plurals.notification_exceptions_count,
                    uiState.channelExceptions,
                    uiState.channelExceptions
                ),
                isChecked = uiState.settings.channels,
                leadingIcon = Icons.Outlined.Campaign,
                onClick = {
                    navBackStack.add(
                        AppRoute.SettingsNotificationCategory(ChatFolderCategory.CHANNELS)
                    )
                },
                onCheckedChange = viewModel::toggleChannels
            )
        }
        
        SectionContainer {
            SectionItem(
                headlineText = stringResource(R.string.notification_reset),
                supportingText = stringResource(R.string.notification_reset_description),
                onClick = viewModel::showResetDialog
            )
        }
    }
    
    if (uiState.showResetDialog) {
        AppDialog(
            title = stringResource(R.string.notification_reset),
            onDismissRequest = viewModel::hideResetDialog,
            buttons = {
                TextButton(onClick = viewModel::hideResetDialog) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(onClick = viewModel::resetSettings) {
                    Text(stringResource(R.string.notification_reset_action))
                }
            },
            content = {
                Text(
                    text = stringResource(R.string.notification_reset_confirm),
                    lineHeight = 18.sp
                )
            }
        )
    }
}
