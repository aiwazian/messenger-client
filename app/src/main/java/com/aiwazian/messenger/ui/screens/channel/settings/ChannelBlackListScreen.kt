/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.CheckCircle
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
import com.aiwazian.messenger.utils.VibrationPattern
import kotlinx.coroutines.launch

@Composable
fun ChannelBlackListScreen(
    channelId: Long,
    viewModel: ChannelSettingsViewModel = hiltViewModel()
) {
    val navHost = LocalNavHost.current
    var searchQuery by remember { mutableStateOf("") }
    var bannedUsers by remember { mutableStateOf(emptyList<User>()) }
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    fun loadBannedUsers() {
        scope.launch {
            bannedUsers = viewModel.getBannedUsers(searchQuery.ifBlank { null })
        }
    }
    
    LaunchedEffect(
        channelId,
        searchQuery
    ) {
        viewModel.init(channelId)
        loadBannedUsers()
    }
    
    var userToUnban by remember { mutableStateOf<User?>(null) }
    
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
                title = { Text(text = stringResource(R.string.removed_user)) },
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
                    items(bannedUsers) { user ->
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
                                            text = { Text("Разблокировать") },
                                            onClick = {
                                                showMenu = false
                                                userToUnban = user
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Rounded.CheckCircle, null)
                                            },
                                            colors = MenuItemColors(
                                                textColor = MaterialTheme.colorScheme.primary,
                                                leadingIconColor = MaterialTheme.colorScheme.primary,
                                                trailingIconColor = MaterialTheme.colorScheme.primary,
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
                            }
                        )
                    }
                }
            }
        }
        
        userToUnban?.let { user ->
            CustomDialog(
                title = "Разблокировать пользователя",
                onDismissRequest = { userToUnban = null },
                buttons = {
                    TextButton(onClick = { userToUnban = null }) {
                        Text(stringResource(R.string.no))
                    }
                    TextButton(
                        onClick = {
                            viewModel.unbanUser(user.id) { success ->
                                userToUnban = null
                                if (success) {
                                    loadBannedUsers()
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Пользователь разблокирован")
                                    }
                                } else {
                                    viewModel.vibrate(VibrationPattern.Error)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Не удалось разблокировать пользователя")
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(stringResource(R.string.yes))
                    }
                },
                content = {
                    Text("Вы точно хотите разблокировать пользователя?")
                }
            )
        }
    }
}
