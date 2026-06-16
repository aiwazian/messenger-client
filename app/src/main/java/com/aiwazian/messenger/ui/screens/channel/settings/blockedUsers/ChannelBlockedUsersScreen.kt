/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.blockedUsers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.components.CustomDialog
import com.aiwazian.messenger.ui.components.CustomDropdownMenu
import com.aiwazian.messenger.ui.components.CustomSnackbar
import com.aiwazian.messenger.ui.components.ProfileCard
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar

@Composable
fun ChannelBlockedUsersScreen(
    channelId: Long,
    viewModel: ChannelBlockedUsersViewModel = hiltViewModel()
) {
    val navBackStack = LocalNavBackStack.current
    val state by viewModel.uiState.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(channelId) {
        viewModel.init(channelId)
    }
    
    Scaffold(
        snackbarHost = {
            CustomSnackbar(snackbarHostState)
        },
        topBar = {
            PageTopBar(
                title = { Text(text = stringResource(R.string.removed_user)) },
                navigationIcon = NavigationIcon(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = navBackStack::removeLastOrNull
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SectionContainer {
                LazyColumn {
                    items(state.blockedUsers) { user ->
                        ProfileCard(
                            id = user.id,
                            headlineText = "${user.firstName} ${user.lastName.orEmpty()}".trim(),
                            avatarUri = user.avatars.firstOrNull()?.uri,
                            trailingContent = {
                                var showMenu by remember { mutableStateOf(false) }
                                
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
                                        text = { Text(stringResource(R.string.unblock)) },
                                        onClick = {
                                            showMenu = false
                                            viewModel.onUnblockClick(user)
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
                            },
                            onClick = {
                                navBackStack.add(
                                    AppRoute.Chat(
                                        user.id,
                                        "${user.firstName} ${user.lastName.orEmpty()}"
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
        
        if (state.showUnblockDialog) {
            CustomDialog(
                title = stringResource(R.string.unblock),
                onDismissRequest = viewModel::hideUnblockDialog,
                buttons = {
                    TextButton(onClick = viewModel::hideUnblockDialog) {
                        Text(stringResource(R.string.no))
                    }
                    TextButton(
                        onClick = viewModel::confirmUnblock,
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
