/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.invites

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AddLink
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.InviteLink
import com.aiwazian.messenger.extensions.toInstance
import com.aiwazian.messenger.extensions.toPrettyDateWithYear
import com.aiwazian.messenger.ui.components.CustomDialog
import com.aiwazian.messenger.ui.components.CustomDropdownMenu
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
    
    LaunchedEffect(channelId) {
        viewModel.init(channelId)
    }
    
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }
    
    val activeInviteLinks by viewModel.activeInviteLinks.collectAsState()
    val inactiveInviteLinks by viewModel.inactiveInviteLinks.collectAsState()
    val expandedMenuId by viewModel.expandedMenuId.collectAsState()
    val isShareSheetVisible by viewModel.isShareSheetVisible.collectAsState()
    val availableChats by viewModel.availableChats.collectAsState()
    val selectedChatIds by viewModel.selectedChatIds.collectAsState()
    val navBackStack = LocalNavBackStack.current
    
    if (viewModel.deleteDialog.isVisible) {
        CustomDialog(
            title = stringResource(R.string.delete),
            onDismissRequest = viewModel.deleteDialog::hide,
            buttons = {
                TextButton(onClick = viewModel.deleteDialog::hide) {
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
    
    if (isShareSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = viewModel::hideShareSheet,
            sheetState = rememberModalBottomSheetState(),
            dragHandle = null
        ) {
            Box {
                LazyColumn {
                    items(availableChats) { chat ->
                        val isSelected = selectedChatIds.contains(chat.id)
                        ListItem(
                            modifier = Modifier.clickable { viewModel.toggleChatSelection(chat.id) },
                            headlineContent = { Text(chat.chatName.asString()) },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Rounded.AccountCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp)
                                )
                            },
                            trailingContent = {
                                Icon(
                                    imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
                
                Column(modifier = Modifier.align(Alignment.BottomCenter)) {
                    AnimatedVisibility(
                        visible = selectedChatIds.isNotEmpty(),
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
                        TextButton(
                            onClick = viewModel::sendLink,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clip(MaterialTheme.shapes.medium),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(stringResource(R.string.send))
                        }
                    }
                }
            }
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            
            if (activeInviteLinks.isNotEmpty()) {
                item {
                    SectionContainer(header = {
                        SectionHeader(title = stringResource(R.string.active_links))
                    }) {
                        activeInviteLinks.forEach { link ->
                            InviteLinkItem(link, viewModel, expandedMenuId)
                        }
                    }
                }
            }
            
            if (inactiveInviteLinks.isNotEmpty()) {
                item {
                    SectionContainer(header = {
                        SectionHeader(title = stringResource(R.string.inactive_links))
                    }) {
                        inactiveInviteLinks.forEach { link ->
                            InviteLinkItem(link, viewModel, expandedMenuId)
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
    val remainingUsesText = if (link.maxUses == null) {
        "∞"
    } else {
        (link.maxUses - link.uses).toString()
    }
    
    val expirationText = if (link.expiresAt == null) {
        "Бессрочна"
    } else {
        link.expiresAt.toLongOrNull()?.toInstance()?.toPrettyDateWithYear() ?: "Бессрочна"
    }
    
    val supportingText = "Осталось $remainingUsesText • $expirationText"
    
    SectionItem(
        headlineText = link.link.removePrefix("https://"),
        supportingText = supportingText,
        trailingContent = {
            Box {
                IconButton(onClick = { viewModel.toggleMenu(link.id) }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = null)
                }
                CustomDropdownMenu(
                    expanded = expandedMenuId == link.id,
                    onDismissRequest = { viewModel.toggleMenu(null) }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.share)) },
                        onClick = { viewModel.shareLink(link.link) },
                        leadingIcon = { Icon(Icons.Rounded.Share, null) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.copy)) },
                        onClick = { viewModel.copyLink(link.link) },
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
