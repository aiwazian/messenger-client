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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.ChannelType
import com.aiwazian.messenger.ui.components.CustomDialog
import com.aiwazian.messenger.ui.components.FramelessTextBox
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavHost
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import com.aiwazian.messenger.ui.screens.main.MainViewModel
import com.aiwazian.messenger.utils.VibrationPattern
import kotlinx.coroutines.launch

@Composable
fun ChannelSettingsScreen(
    channelId: Long,
    channelViewModel: ChannelSettingsViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val navHost = LocalNavHost.current
    LaunchedEffect(channelId) {
        channelViewModel.init(channelId)
    }
    
    val channel by channelViewModel.channelInfo.collectAsState()
    val hasChanges by channelViewModel.hasChanges.collectAsState()
    
    val scrollState = rememberScrollState()
    
    val scope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            val actions = if (hasChanges) {
                listOf(
                    TopBarAction(
                        icon = Icons.Rounded.Check,
                        onClick = {
                            scope.launch {
                                val savedId = channelViewModel.trySave()
                                if (savedId != null) {
                                    navHost.removeLastOrNull()
                                }
                            }
                        })
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
                    value = channel.name,
                    onValueChange = channelViewModel::changeName
                )
                FramelessTextBox(
                    placeholder = stringResource(R.string.description),
                    value = channel.bio.orEmpty(),
                    onValueChange = channelViewModel::changeBio
                )
            }
            
            SectionContainer {
                SectionItem(
                    leadingIcon = Icons.Rounded.LockOpen,
                    headlineText = stringResource(R.string.channel_type),
                    trailingText = if (channel.channelType == ChannelType.PUBLIC) {
                        stringResource(R.string.public_channel)
                    } else {
                        stringResource(R.string.private_channel)
                    },
                    onClick = {
                        navHost.add(AppRoute.ChannelTypeSettings(channelId = channel.id))
                    })
            }
            
            SectionContainer {
                SectionItem(
                    leadingIcon = Icons.Rounded.People,
                    headlineText = stringResource(R.string.subscribers),
                    trailingText = channel.subscribers.toString(),
                    onClick = {
                        navHost.add(AppRoute.ChannelSubscribers(channel.id))
                    })
                SectionItem(
                    leadingIcon = Icons.Rounded.RemoveCircleOutline,
                    headlineText = stringResource(R.string.removed_user),
                    trailingText = channel.removedUser?.toString(),
                    onClick = {
                        navHost.add(AppRoute.ChannelBlackList(channel.id))
                    })
            }
            
            SectionContainer {
                SectionItem(
                    headlineText = stringResource(R.string.delete_channel),
                    contentColor = MaterialTheme.colorScheme.error,
                    onClick = channelViewModel.deleteDialog::show
                )
            }
            
            if (channelViewModel.deleteDialog.isVisible) {
                CustomDialog(
                    title = stringResource(R.string.delete_channel),
                    onDismissRequest = channelViewModel.deleteDialog::hide,
                    buttons = {
                        TextButton(onClick = channelViewModel.deleteDialog::hide) {
                            Text(stringResource(R.string.cancel))
                        }
                        TextButton(
                            onClick = {
                                scope.launch {
                                    val isDeleted = channelViewModel.tryDelete()
                                    
                                    if (isDeleted) {
                                        mainViewModel.deleteChat(channel.id)
                                        channelViewModel.deleteDialog.hide()
                                        navHost.clear()
                                        navHost.add(AppRoute.Main)
                                    } else {
                                        channelViewModel.vibrate(VibrationPattern.Error)
                                    }
                                }
                            },
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
    val navHost = LocalNavHost.current
    
    PageTopBar(
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            onClick = navHost::removeLastOrNull
        ),
        actions = actions
    )
}
