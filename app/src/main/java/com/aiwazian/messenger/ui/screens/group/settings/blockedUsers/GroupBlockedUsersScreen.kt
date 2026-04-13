/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.blockedUsers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.User
import com.aiwazian.messenger.ui.components.CustomDialog
import com.aiwazian.messenger.ui.components.CustomDropdownMenu
import com.aiwazian.messenger.ui.components.CustomSnackbar
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavHost
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.utils.DialogController

@Composable
fun GroupBlockedUsersScreen(
    groupId: Long,
    viewModel: GroupBlockedUsersViewModel = hiltViewModel()
) {
    val navHost = LocalNavHost.current
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val unblockDialogController = remember { DialogController() }
    
    LaunchedEffect(groupId) {
        viewModel.init(groupId)
    }
    
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is GroupBlockedUsersSideEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                
                GroupBlockedUsersSideEffect.ShowUnblockConfirmation -> {
                    unblockDialogController.show()
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            PageTopBar(
                title = { Text(stringResource(R.string.removed_user)) },
                navigationIcon = NavigationIcon(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = navHost::removeLastOrNull
                )
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                CustomSnackbar(text = data.visuals.message, onDismiss = data::dismiss)
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()) {
            if (state.isLoading && state.blockedUsers.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn {
                    items(state.blockedUsers) { user ->
                        BlockedUserItem(
                            user = user,
                            onUnblock = { viewModel.onUnblockClick(user) }
                        )
                    }
                }
            }
        }
    }
    
    if (unblockDialogController.isVisible) {
        CustomDialog(
            title = "Разблокировать",
            onDismissRequest = unblockDialogController::hide,
            buttons = {
                TextButton(onClick = unblockDialogController::hide) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(onClick = {
                    viewModel.confirmUnblock()
                    unblockDialogController.hide()
                }) {
                    Text(stringResource(R.string.ok))
                }
            }
        ) {
            Text("Вы уверены, что хотите разблокировать этого пользователя?")
        }
    }
}

@Composable
fun BlockedUserItem(
    user: User,
    onUnblock: () -> Unit
) {
    val navHost = LocalNavHost.current
    var showMenu by remember { mutableStateOf(false) }
    
    SectionItem(
        headlineText = "${user.firstName} ${user.lastName.orEmpty()}".trim(),
        supportingText = user.username?.let { "@$it" },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = null)
                }
                CustomDropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Разблокировать") },
                        onClick = {
                            showMenu = false
                            onUnblock()
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
                        ),
                    )
                }
            }
        },
        onClick = {
            navHost.add(AppRoute.Chat(user.id))
        }
    )
}
