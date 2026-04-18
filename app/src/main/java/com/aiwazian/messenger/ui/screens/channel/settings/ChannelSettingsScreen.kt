/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.ChannelType
import com.aiwazian.messenger.ui.components.CustomDialog
import com.aiwazian.messenger.ui.components.FramelessTextBox
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.components.topBar.TopBarAction

@Composable
fun ChannelSettingsScreen(
    channelId: Long,
    viewModel: ChannelSettingsViewModel = hiltViewModel()
) {
    val navBackStack = LocalNavBackStack.current
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                ChannelSettingsEffect.NavigateToMain -> {
                    navBackStack.clear()
                    navBackStack.add(AppRoute.Main)
                }
                
                ChannelSettingsEffect.NavigateToBack -> navBackStack.removeLastOrNull()
                
                is ChannelSettingsEffect.ShowSnackbar -> {
                
                }
            }
        }
    }
    
    LaunchedEffect(channelId) {
        viewModel.init(channelId)
    }
    
    val scrollState = rememberScrollState()
    
    Scaffold(
        topBar = {
            val actions = if (uiState.hasChanges) {
                listOf(
                    TopBarAction(
                        icon = Icons.Rounded.Check,
                        onClick = viewModel::save
                    )
                )
            } else emptyList()
            
            TopBar(actions)
        },
        modifier = Modifier.imePadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {
            SectionContainer {
                FramelessTextBox(
                    placeholder = stringResource(R.string.channel_name),
                    value = uiState.channel.name,
                    onValueChange = viewModel::changeName
                )
                FramelessTextBox(
                    placeholder = stringResource(R.string.description),
                    value = uiState.channel.bio.orEmpty(),
                    onValueChange = viewModel::changeBio
                )
            }
            
            SectionContainer {
                SectionItem(
                    leadingIcon = Icons.Outlined.Lock,
                    headlineText = stringResource(R.string.channel_type),
                    trailingText = if (uiState.channel.channelType == ChannelType.PUBLIC) {
                        stringResource(R.string.public_channel)
                    } else {
                        stringResource(R.string.private_channel)
                    },
                    onClick = {
                        navBackStack.add(AppRoute.ChannelTypeSettings(channelId = uiState.channel.id))
                    })
            }
            
            SectionContainer {
                SectionItem(
                    leadingIcon = Icons.Rounded.People,
                    headlineText = stringResource(R.string.subscribers),
                    trailingText = uiState.channel.subscribers.toString(),
                    onClick = {
                        navBackStack.add(AppRoute.ChannelSubscribers(uiState.channel.id))
                    })
                SectionItem(
                    leadingIcon = Icons.Rounded.Block,
                    headlineText = stringResource(R.string.removed_user),
                    trailingText = uiState.channel.removedUser?.toString(),
                    onClick = {
                        navBackStack.add(AppRoute.ChannelBlackList(uiState.channel.id))
                    })
            }
            
            SectionContainer {
                SectionItem(
                    headlineText = stringResource(R.string.delete_channel),
                    contentColor = MaterialTheme.colorScheme.error,
                    onClick = viewModel::showDeleteDialog
                )
            }
            
            if (uiState.showDeleteDialog) {
                CustomDialog(
                    title = stringResource(R.string.delete_channel),
                    onDismissRequest = viewModel::hideDeleteDialog,
                    buttons = {
                        TextButton(onClick = viewModel::hideDeleteDialog) {
                            Text(stringResource(R.string.cancel))
                        }
                        TextButton(
                            onClick = viewModel::delete,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(stringResource(R.string.delete_channel))
                        }
                    },
                    content = {
                        Text("Вы точно хотите удалить канал?")
                    })
            }
        }
    }
}

@Composable
private fun TopBar(actions: List<TopBarAction>) {
    val navBackStack = LocalNavBackStack.current
    
    PageTopBar(
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            onClick = navBackStack::removeLastOrNull
        ),
        actions = actions
    )
}
