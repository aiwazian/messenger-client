/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.transfer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.app.AppDialog
import com.aiwazian.messenger.ui.app.AppSnackbar
import com.aiwazian.messenger.ui.components.ProfileCard
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun GroupTransferOwnershipScreen(
    groupId: Long,
    viewModel: GroupTransferOwnershipViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val navBackStack = LocalNavBackStack.current
    
    LaunchedEffect(groupId) {
        viewModel.init(groupId)
    }
    
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var snackbarJob by remember { mutableStateOf<Job?>(null) }
    
    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                GroupTransferOwnershipEffect.NavigateToMain -> {
                    navBackStack.clear()
                    navBackStack.add(AppRoute.Main)
                }
                
                is GroupTransferOwnershipEffect.ShowSnackbar -> {
                    snackbarJob?.cancel()
                    snackbarJob = scope.launch {
                        snackbarHostState.showSnackbar(effect.message.asString(context))
                    }
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            PageTopBar(
                title = { Text(stringResource(R.string.select_new_owner)) },
            )
        },
        snackbarHost = {
            AppSnackbar(snackbarHostState)
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (!uiState.isLoading && uiState.members.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.no_users_available))
                }
            } else {
                SectionContainer {
                    LazyColumn {
                        items(items = uiState.members) { user ->
                            ProfileCard(
                                id = user.id,
                                headlineText = "${user.firstName} ${user.lastName.orEmpty()}".trim(),
                                avatarUri = user.avatars.firstOrNull()?.uri,
                                supportingText = user.username?.let { "@$it" },
                                onClick = { viewModel.selectUser(user) }
                            )
                        }
                    }
                }
            }
        }
        
        if (uiState.selectedUser != null) {
            AppDialog(
                title = stringResource(R.string.transfer_ownership),
                onDismissRequest = viewModel::clearSelection,
                buttons = {
                    TextButton(onClick = viewModel::clearSelection) {
                        Text(stringResource(R.string.cancel))
                    }
                    
                    TextButton(
                        onClick = viewModel::confirmTransfer,
                        enabled = !uiState.isTransferring
                    ) {
                        Text(stringResource(R.string.yes))
                    }
                },
                content = {
                    Text(stringResource(R.string.transfer_ownership_confirm_message))
                }
            )
        }
    }
}
