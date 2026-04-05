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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.User
import com.aiwazian.messenger.ui.components.CustomDialog
import com.aiwazian.messenger.ui.components.CustomSnackbar
import com.aiwazian.messenger.ui.components.DropdownMenu
import com.aiwazian.messenger.ui.components.FramelessTextBox
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavHost
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.screens.channel.settings.ChannelSettingsViewModel
import com.aiwazian.messenger.utils.VibrationPattern
import kotlinx.coroutines.launch

@Composable
fun ChannelSubscribersScreen(
    channelId: Long,
    viewModel: ChannelSettingsViewModel = hiltViewModel()
) {
    val navHost = LocalNavHost.current
    var searchQuery by remember { mutableStateOf("") }
    var subscribers by remember { mutableStateOf(emptyList<User>()) }
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    fun loadSubscribers() {
        scope.launch {
            subscribers = viewModel.getSubscribers(searchQuery.ifBlank { null })
        }
    }
    
    LaunchedEffect(
        channelId,
        searchQuery
    ) {
        viewModel.init(channelId)
        loadSubscribers()
    }
    
    var userToKick by remember { mutableStateOf<User?>(null) }
    var userToBan by remember { mutableStateOf<User?>(null) }
    
    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) {
                CustomSnackbar(
                    text = it.visuals.message,
                    onDismiss = it::dismiss
                )
            }
        },
        topBar = {
            PageTopBar(
                title = { Text(stringResource(R.string.subscribers)) },
                navigationIcon = NavigationIcon(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = navHost::removeLastOrNull
                )
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            FramelessTextBox(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = stringResource(R.string.search)
            )
            
            SectionContainer {
                LazyColumn {
                    items(subscribers) { user ->
                        SectionItem(
                            headlineText = "${user.firstName} ${user.lastName.orEmpty()}",
                            trailingContent = {
                                var showMenu by remember { mutableStateOf(false) }
                                
                                Box {
                                    IconButton(onClick = { showMenu = true }) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = null
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Выгнать") },
                                            onClick = {
                                                showMenu = false
                                                userToKick = user
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
                                            text = { Text("Заблокировать") },
                                            onClick = {
                                                showMenu = false
                                                userToBan = user
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
                                navHost.add(AppRoute.Chat(user.id))
                            })
                    }
                }
            }
        }
        
        userToKick?.let { user ->
            CustomDialog(
                title = "Выгнать участника",
                onDismissRequest = { userToKick = null },
                buttons = {
                    TextButton(onClick = { userToKick = null }) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(
                        onClick = {
                            viewModel.kickUser(user.id) { success ->
                                if (success) {
                                    userToKick = null
                                    loadSubscribers()
                                } else {
                                    viewModel.vibrate(VibrationPattern.Error)
                                }
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Выгнать")
                    }
                },
                content = {
                    Text("Вы уверены, что хотите выгнать ${user.firstName} из канала?")
                }
            )
        }
        
        userToBan?.let { user ->
            CustomDialog(
                title = "Заблокировать подписчика",
                onDismissRequest = { userToBan = null },
                buttons = {
                    TextButton(onClick = { userToBan = null }) {
                        Text("Нет")
                    }
                    TextButton(
                        onClick = {
                            viewModel.banUser(user.id) { success ->
                                userToBan = null
                                if (success) {
                                    loadSubscribers()
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Пользователь заблокирован")
                                    }
                                } else {
                                    viewModel.vibrate(VibrationPattern.Error)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Не удалось заблокировать пользователя")
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Да")
                    }
                },
                content = {
                    Text("Вы точно хотите заблокировать подписчика?")
                }
            )
        }
    }
}
