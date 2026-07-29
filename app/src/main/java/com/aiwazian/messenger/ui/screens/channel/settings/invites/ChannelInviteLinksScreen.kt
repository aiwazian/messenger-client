/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.invites

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddLink
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.InviteLink
import com.aiwazian.messenger.extensions.toInstance
import com.aiwazian.messenger.extensions.toPrettyDateWithYear
import com.aiwazian.messenger.ui.app.AppDialog
import com.aiwazian.messenger.ui.app.AppDropdownMenu
import com.aiwazian.messenger.ui.app.AppSnackbar
import com.aiwazian.messenger.ui.components.ShareBottomSheet
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionHeader
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelInviteLinksScreen(
    channelId: Long,
    viewModel: ChannelInviteLinksViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(channelId) {
        viewModel.init(channelId)
    }
    
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }
    
    val navBackStack = LocalNavBackStack.current
    
    if (uiState.linkIdToDelete != null) {
        AppDialog(
            title = stringResource(R.string.delete),
            onDismissRequest = viewModel::hideDeleteDialog,
            buttons = {
                TextButton(onClick = viewModel::hideDeleteDialog) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(
                    onClick = viewModel::confirmDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            content = {
                Text("Вы уверены, что хотите удалить эту ссылку?")
            }
        )
    }
    
    if (uiState.showShareSheet) {
        ShareBottomSheet(
            items = uiState.availableChats,
            onItemClick = viewModel::toggleChatSelection,
            onSendClick = viewModel::sendLink,
            onDismiss = viewModel::hideShareSheet
        )
    }
    
    Scaffold(
        snackbarHost = {
            AppSnackbar(snackbarHostState)
        },
        topBar = {
            PageTopBar(
                title = { Text(stringResource(R.string.invite_links)) },
                navigationIcon = NavigationIcon(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = navBackStack::removeLastOrNull
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                SectionContainer {
                    SectionItem(
                        headlineText = stringResource(R.string.new_link),
                        leadingIcon = Icons.Rounded.AddLink,
                        contentColor = MaterialTheme.colorScheme.primary,
                        onClick = {
                            navBackStack.add(AppRoute.CreateInviteLink(channelId))
                        }
                    )
                }
            }
            
            if (uiState.activeLinks.isNotEmpty()) {
                item {
                    SectionContainer(header = {
                        SectionHeader(title = stringResource(R.string.active_links))
                    }) {
                        uiState.activeLinks.forEach { link ->
                            InviteLinkItem(link, viewModel, uiState.expandedMenuId)
                        }
                    }
                }
            }
            
            if (uiState.inactiveLinks.isNotEmpty()) {
                item {
                    SectionContainer(header = {
                        SectionHeader(title = stringResource(R.string.inactive_links))
                    }) {
                        uiState.inactiveLinks.forEach { link ->
                            InviteLinkItem(link, viewModel, uiState.expandedMenuId)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InviteLinkItem(
    link: InviteLink,
    viewModel: ChannelInviteLinksViewModel,
    expandedMenuId: Long?
) {
    val remainingUsesText = if (link.maxUses == null || link.uses == null) {
        "∞"
    } else {
        (link.maxUses - link.uses).toString()
    }
    
    val expirationText = if (link.expiresAt == null) {
        "Бессрочна"
    } else {
        link.expiresAt.toInstance().toPrettyDateWithYear()
    }
    
    val supportingText = "Осталось $remainingUsesText • $expirationText"
    
    SectionItem(
        headlineText = "aiwazian.ru/" + link.code,
        supportingText = supportingText,
        trailingContent = {
            Box {
                var expanded by remember { mutableStateOf(false) }
                val isMenuExpanded = expandedMenuId == link.id
                
                IconButton(onClick = {
                    expanded = true
                    viewModel.setExpandedMenuId(link.id)
                }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = null)
                }
                AppDropdownMenu(
                    expanded = isMenuExpanded && expanded,
                    onDismissRequest = {
                        expanded = false
                        viewModel.setExpandedMenuId(null)
                    }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.share)) },
                        onClick = { viewModel.shareLink(link.id) },
                        leadingIcon = { Icon(Icons.Rounded.Share, null) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.copy)) },
                        onClick = { viewModel.copyLink(link.id) },
                        leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.delete),
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = { viewModel.showDeleteConfirmation(link.id) },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    )
}