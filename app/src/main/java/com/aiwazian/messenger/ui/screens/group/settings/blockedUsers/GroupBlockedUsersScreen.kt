/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.blockedUsers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MoreVert
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
import com.aiwazian.messenger.domain.User
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
fun GroupBlockedUsersScreen(
    groupId: Long,
    viewModel: GroupBlockedUsersViewModel = hiltViewModel()
) {
    LaunchedEffect(groupId) {
        viewModel.init(groupId)
    }
    
    val context = LocalContext.current
    val navBackStack = LocalNavBackStack.current
    
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is GroupBlockedUsersSideEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message.asString(context))
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
                    onClick = navBackStack::removeLastOrNull
                )
            )
        },
        snackbarHost = {
            CustomSnackbar(snackbarHostState)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SectionContainer {
                LazyColumn {
                    items(uiState.blockedUsers) { user ->
                        BlockedUserItem(
                            user = user,
                            onUnblock = { viewModel.onUnblockClick(user) }
                        )
                    }
                }
            }
        }
    }
    
    if (uiState.showUnblockDialog) {
        CustomDialog(
            title = stringResource(R.string.unblock),
            onDismissRequest = viewModel::hideUnblockDialog,
            buttons = {
                TextButton(onClick = viewModel::hideUnblockDialog) {
                    Text(stringResource(R.string.no))
                }
                TextButton(onClick = viewModel::confirmUnblock) {
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
    val navBackStack = LocalNavBackStack.current
    var showMenu by remember { mutableStateOf(false) }
    
    ProfileCard(
        id = user.id,
        headlineText = "${user.firstName} ${user.lastName.orEmpty()}".trim(),
        avatarUri = user.avatars.firstOrNull()?.uri,
        supportingText = user.username?.let { "@$it" },
        trailingContent = {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = null)
            }
            CustomDropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.unblock)) },
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
        },
        onClick = {
            navBackStack.add(AppRoute.Chat(user.id, "${user.firstName} ${user.lastName.orEmpty()}"))
        }
    )
}
