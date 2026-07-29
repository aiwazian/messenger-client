/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.privacy.blockedUsers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.User
import com.aiwazian.messenger.ui.app.AppDialog
import com.aiwazian.messenger.ui.app.AppDropdownMenu
import com.aiwazian.messenger.ui.app.AppSnackbar
import com.aiwazian.messenger.ui.components.ProfileCard
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun BlockedUserListScreen(
    viewModel: BlockedUserListViewModel = hiltViewModel()
) {
    val navBackStack = LocalNavBackStack.current
    val uiState by viewModel.uiState.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarJob by remember { mutableStateOf<Job?>(null) }
    
    LaunchedEffect(Unit) {
        viewModel.init()
    }
    
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is BlockedUserListSideEffect.ShowSnackbar -> {
                    snackbarJob?.cancel()
                    snackbarJob = launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar(
                            message = effect.message,
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            PageTopBar(
                title = { Text(stringResource(R.string.blocked_users)) },
                navigationIcon = NavigationIcon(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = navBackStack::removeLastOrNull
                )
            )
        },
        snackbarHost = {
            AppSnackbar(snackbarHostState)
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularWavyProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    if (uiState.blockedUsers.isNotEmpty()) {
                        SectionContainer {
                            uiState.blockedUsers.forEach { user ->
                                BlockedUserItem(
                                    user = user,
                                    onUnblockClick = { viewModel.showUnblockDialog(user) },
                                    onProfileClick = {
                                        val name = buildString {
                                            append(user.firstName)
                                            if (!user.lastName.isNullOrBlank()) {
                                                append(" ")
                                                append(user.lastName)
                                            }
                                        }
                                        val currentAvatarUri =
                                            user.avatars.maxByOrNull { it.sortOrder }?.uri
                                        navBackStack.add(
                                            AppRoute.Profile(
                                                profileId = user.id,
                                                profileName = name,
                                                avatarUri = currentAvatarUri?.toString()
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Нет заблокированных пользователей",
                                modifier = Modifier.padding(top = 32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    
    if (uiState.showUnblockDialog && uiState.selectedUserToUnblock != null) {
        AppDialog(
            title = stringResource(R.string.unblock),
            onDismissRequest = viewModel::hideUnblockDialog,
            buttons = {
                TextButton(onClick = viewModel::hideUnblockDialog) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(onClick = viewModel::unblockUser) {
                    Text(stringResource(R.string.unblock))
                }
            },
            content = {
                Text(
                    text = "Вы уверены, что хотите разблокировать пользователя ${uiState.selectedUserToUnblock?.firstName}?",
                    lineHeight = 18.sp
                )
            }
        )
    }
}

@Composable
private fun BlockedUserItem(
    user: User,
    onUnblockClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    
    val name = buildString {
        append(user.firstName)
        if (!user.lastName.isNullOrBlank()) {
            append(" ")
            append(user.lastName)
        }
    }
    
    val currentAvatarUri = user.avatars.maxByOrNull { it.sortOrder }?.uri
    
    ProfileCard(
        id = user.id,
        headlineText = name,
        supportingText = user.username?.let { "@$it" },
        avatarUri = currentAvatarUri,
        onClick = onProfileClick,
        trailingContent = {
            Box {
                IconButton(onClick = { isMenuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "More"
                    )
                }
                AppDropdownMenu(
                    expanded = isMenuExpanded,
                    onDismissRequest = { isMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.unblock)) },
                        onClick = {
                            isMenuExpanded = false
                            onUnblockClick()
                        }
                    )
                }
            }
        }
    )
}
