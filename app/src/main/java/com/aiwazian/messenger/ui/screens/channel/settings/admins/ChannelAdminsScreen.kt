/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.admins

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.ChannelAdmin
import com.aiwazian.messenger.ui.app.AppDialog
import com.aiwazian.messenger.ui.app.AppDropdownMenu
import com.aiwazian.messenger.ui.app.AppDropdownMenuItem
import com.aiwazian.messenger.ui.app.AppScaffold
import com.aiwazian.messenger.ui.app.AppSnackbar
import com.aiwazian.messenger.ui.components.ProfileCard
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.PageTopBar

@Composable
fun ChannelAdminsScreen(
    channelId: Long,
    viewModel: ChannelAdminsViewModel = hiltViewModel()
) {
    val navBackStack = LocalNavBackStack.current
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(channelId) {
        viewModel.init(channelId)
    }
    
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is ChannelAdminsSideEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message.asString(context))
                }
            }
        }
    }
    
    AppScaffold(
        topBar = {
            PageTopBar(
                title = {
                    Text(stringResource(R.string.administrators))
                }
            )
        },
        snackbarHost = {
            AppSnackbar(snackbarHostState)
        }
    ) {
        SectionContainer {
            SectionItem(
                leadingIcon = Icons.Rounded.PersonAdd,
                headlineText = stringResource(R.string.add_administrator),
                onClick = { navBackStack.add(AppRoute.AddChannelAdmin(channelId)) }
            )
        }
        
        SectionContainer {
            if (uiState.admins.isEmpty()) {
                SectionItem(headlineText = stringResource(R.string.no_administrators))
            } else {
                LazyColumn {
                    items(uiState.admins) { admin ->
                        ChannelAdminItem(
                            admin = admin,
                            isCurrentUser = admin.userId == uiState.currentUserId,
                            onOpenPermissions = {
                                navBackStack.add(
                                    AppRoute.ChannelAdminPermissions(channelId, admin.userId)
                                )
                            },
                            onDemote = { viewModel.showDemoteDialog(admin.userId) }
                        )
                    }
                }
            }
        }
    }
    
    if (uiState.showDemoteDialog) {
        AppDialog(
            title = stringResource(R.string.dismiss_admin),
            onDismissRequest = viewModel::hideDemoteDialog,
            buttons = {
                TextButton(onClick = viewModel::hideDemoteDialog) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(
                    onClick = viewModel::confirmDemote,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.dismiss_admin))
                }
            }
        ) {
            Text(stringResource(R.string.dismiss_admin_confirm_message))
        }
    }
}

@Composable
private fun ChannelAdminItem(
    admin: ChannelAdmin,
    isCurrentUser: Boolean,
    onOpenPermissions: () -> Unit,
    onDemote: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    ProfileCard(
        id = admin.userId,
        headlineText = "${admin.firstName} ${admin.lastName.orEmpty()}".trim(),
        supportingText = admin.username?.let { "@$it" },
        trailingContent = {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = null)
            }
            AppDropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                if (isCurrentUser) {
                    AppDropdownMenuItem(
                        text = stringResource(R.string.view_permissions),
                        onClick = {
                            showMenu = false
                            onOpenPermissions()
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.Tune, null)
                        }
                    )
                } else {
                    AppDropdownMenuItem(
                        text = stringResource(R.string.edit_permissions),
                        onClick = {
                            showMenu = false
                            onOpenPermissions()
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.Tune, null)
                        }
                    )
                    AppDropdownMenuItem(
                        text = stringResource(R.string.dismiss_admin),
                        onClick = {
                            showMenu = false
                            onDemote()
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.PersonRemove, null)
                        },
                        contentColor = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        onClick = onOpenPermissions
    )
}
