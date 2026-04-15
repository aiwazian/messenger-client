/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.addMember

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.User
import com.aiwazian.messenger.ui.components.CustomSnackbar
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import com.aiwazian.messenger.ui.components.topBar.PageTopBar

@Composable
fun AddMemberScreen(
    groupId: Long,
    viewModel: AddMemberViewModel = hiltViewModel()
) {
    val navBackStack = LocalNavBackStack.current
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(groupId) {
        viewModel.init(groupId)
    }

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is AddMemberSideEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            PageTopBar(
                title = { Text(stringResource(R.string.add_member)) },
                navigationIcon = NavigationIcon(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = navBackStack::removeLastOrNull
                ),
                actions = listOf(
                    TopBarAction(
                        icon = Icons.Rounded.Check,
                        onClick = {
                            viewModel.addSelectedUsers {
                                navBackStack.removeLastOrNull()
                            }
                        }
                    )
                )
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                CustomSnackbar(text = data.visuals.message, onDismiss = data::dismiss)
            }
        },
        floatingActionButton = {
            if (state.selectedUserIds.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        viewModel.addSelectedUsers {
                            navBackStack.removeLastOrNull()
                        }
                    }
                ) {
                    Icon(Icons.Rounded.PersonAdd, contentDescription = null)
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (state.isLoading && state.users.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.users.isEmpty()) {
                Text(
                    text = "Нет доступных пользователей",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            } else {
                LazyColumn {
                    items(state.users) { user ->
                        UserListItem(
                            user = user,
                            isSelected = state.selectedUserIds.contains(user.id),
                            onClick = { viewModel.toggleUser(user.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserListItem(
    user: User,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${user.firstName} ${user.lastName.orEmpty()}".trim(),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )
        user.username?.let {
            Text(
                text = "@$it",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        Checkbox(
            checked = isSelected,
            onCheckedChange = null
        )
    }
}
