/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.management

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.app.AppDialog
import com.aiwazian.messenger.ui.app.AppSnackbar
import com.aiwazian.messenger.ui.components.CountdownTextButton
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar

@Composable
fun ChannelManagementScreen(
    channelId: Long,
    viewModel: ChannelManagementViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val navBackStack = LocalNavBackStack.current
    
    LaunchedEffect(channelId) {
        viewModel.init(channelId)
    }
    
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                ChannelManagementEffect.NavigateToMain -> {
                    navBackStack.clear()
                    navBackStack.add(AppRoute.Main)
                }
                
                is ChannelManagementEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message.asString(context))
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            PageTopBar(
                title = { Text(stringResource(R.string.channel_management)) },
                navigationIcon = NavigationIcon(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = navBackStack::removeLastOrNull
                )
            )
        },
        snackbarHost = {
            AppSnackbar(snackbarHostState)
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            SectionContainer {
                SectionItem(
                    headlineText = stringResource(R.string.transfer_ownership),
                    onClick = {
                        navBackStack.add(AppRoute.ChannelTransferOwnership(channelId = channelId))
                    }
                )
                
                SectionItem(
                    headlineText = stringResource(R.string.clear_history),
                    onClick = viewModel::showClearHistoryDialog
                )
                
                SectionItem(
                    headlineText = stringResource(R.string.delete_channel),
                    contentColor = MaterialTheme.colorScheme.error,
                    onClick = viewModel::showDeleteDialog
                )
            }
        }
        
        if (uiState.showClearHistoryDialog) {
            AppDialog(
                title = stringResource(R.string.clear_history),
                onDismissRequest = viewModel::hideClearHistoryDialog,
                buttons = {
                    TextButton(onClick = viewModel::hideClearHistoryDialog) {
                        Text(stringResource(R.string.cancel))
                    }
                    
                    CountdownTextButton(
                        text = stringResource(R.string.delete),
                        seconds = 5,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        onClickWhileRunning = viewModel::vibrate,
                        onClickAfterFinish = viewModel::clearHistory
                    )
                },
                content = {
                    Text(stringResource(R.string.clear_history_confirm))
                }
            )
        }
        
        if (uiState.showDeleteDialog) {
            AppDialog(
                title = stringResource(R.string.delete_channel),
                onDismissRequest = viewModel::hideDeleteDialog,
                buttons = {
                    TextButton(onClick = viewModel::hideDeleteDialog) {
                        Text(stringResource(R.string.cancel))
                    }
                    
                    CountdownTextButton(
                        text = stringResource(R.string.delete_channel),
                        seconds = 5,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        onClickWhileRunning = viewModel::vibrate,
                        onClickAfterFinish = viewModel::delete
                    )
                },
                content = {
                    Text(stringResource(R.string.delete_channel_confirm_message))
                }
            )
        }
    }
}
