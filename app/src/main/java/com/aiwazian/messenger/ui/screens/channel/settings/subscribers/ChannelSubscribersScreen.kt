/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.subscribers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.components.CustomDialog
import com.aiwazian.messenger.ui.components.CustomDropdownMenu
import com.aiwazian.messenger.ui.components.CustomSnackbar
import com.aiwazian.messenger.ui.components.FramelessTextBox
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar

@Composable
fun ChannelSubscribersScreen(
    channelId: Long,
    viewModel: ChannelSubscribersViewModel = hiltViewModel()
) {
    val navBackStack = LocalNavBackStack.current
    
    LaunchedEffect(channelId) {
        viewModel.init(channelId)
    }
    
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is ChannelSubscribersSideEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message.asString(context))
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            PageTopBar(
                title = { Text(stringResource(R.string.subscribers)) },
                navigationIcon = NavigationIcon(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = navBackStack::removeLastOrNull
                )
            )
        },
        snackbarHost = {
            CustomSnackbar(snackbarHostState)
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            FramelessTextBox(
                value = uiState.searchQuery,
                onValueChange = viewModel::changeSearchQuery,
                placeholder = stringResource(R.string.search)
            )
            
            SectionContainer {
                LazyColumn {
                    items(items = uiState.subscribers) { user ->
                        SectionItem(
                            headlineText = "${user.firstName} ${user.lastName.orEmpty()}".trim(),
                            trailingContent = {
                                var showMenu by remember { mutableStateOf(false) }
                                
                                Box {
                                    IconButton(onClick = { showMenu = true }) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = null
                                        )
                                    }
                                    CustomDropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.kick)) },
                                            onClick = {
                                                showMenu = false
                                                viewModel.showKickDialog(user.id)
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Rounded.PersonRemove, null)
                                            },
                                            colors = MenuItemColors(
                                                textColor = MaterialTheme.colorScheme.error,
                                                leadingIconColor = MaterialTheme.colorScheme.error,
                                                trailingIconColor = MaterialTheme.colorScheme.error,
                                                disabledTextColor = Color.Unspecified,
                                                disabledLeadingIconColor = Color.Unspecified,
                                                disabledTrailingIconColor = Color.Unspecified
                                            )
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.block_user)) },
                                            onClick = {
                                                showMenu = false
                                                viewModel.showBlockDialog(user.id)
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Rounded.Block, null)
                                            },
                                            colors = MenuItemColors(
                                                textColor = MaterialTheme.colorScheme.error,
                                                leadingIconColor = MaterialTheme.colorScheme.error,
                                                trailingIconColor = MaterialTheme.colorScheme.error,
                                                disabledTextColor = Color.Unspecified,
                                                disabledLeadingIconColor = Color.Unspecified,
                                                disabledTrailingIconColor = Color.Unspecified
                                            )
                                        )
                                    }
                                }
                            },
                            onClick = {
                                navBackStack.add(
                                    AppRoute.Chat(
                                        user.id,
                                        "${user.firstName} ${user.lastName.orEmpty()}".trim()
                                    )
                                )
                            })
                    }
                }
            }
        }
        
        if (uiState.showKickDialog) {
            CustomDialog(
                title = stringResource(R.string.kick),
                onDismissRequest = viewModel::hideKickDialog,
                buttons = {
                    TextButton(onClick = viewModel::hideKickDialog) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(
                        onClick = viewModel::kickUser,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.kick))
                    }
                },
                content = {
                    Text("Вы уверены, что хотите выгнать пользователя из канала?")
                }
            )
        }
    }
    
    if (uiState.showBlockDialog) {
        CustomDialog(
            title = stringResource(R.string.block_user),
            onDismissRequest = viewModel::hideBlockDialog,
            buttons = {
                TextButton(onClick = viewModel::hideBlockDialog) {
                    Text(stringResource(R.string.no))
                }
                TextButton(
                    onClick = viewModel::blockUser,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.yes))
                }
            },
            content = {
                Text("Вы точно хотите заблокировать подписчика?")
            }
        )
    }
}
