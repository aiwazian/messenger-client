/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.notification.exception.chats

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.ChatFolderCategory
import com.aiwazian.messenger.ui.app.AppScaffold
import com.aiwazian.messenger.ui.components.FramelessTextBox
import com.aiwazian.messenger.ui.components.ProfileCard
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.topBar.PageTopBar

@Composable
fun SelectExceptionChatScreen(
    category: ChatFolderCategory,
    viewModel: SelectExceptionChatViewModel = hiltViewModel()
) {
    val navBackStack = LocalNavBackStack.current
    
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(category) {
        viewModel.init(category)
    }
    
    AppScaffold(
        topBar = {
            PageTopBar(
                title = {
                    Text(stringResource(R.string.notification_exception_add))
                }
            )
        }) {
        SectionContainer {
            FramelessTextBox(
                placeholder = stringResource(R.string.search),
                value = uiState.query,
                onValueChange = viewModel::onQueryChange
            )
        }
        
        SectionContainer {
            uiState.chats.forEach { chat ->
                ProfileCard(
                    id = chat.id,
                    headlineText = chat.chatName.asString(),
                    avatarUri = chat.avatarUri,
                    onClick = {
                        navBackStack.add(AppRoute.SettingsNotificationException(chat.id))
                    })
            }
        }
    }
}
