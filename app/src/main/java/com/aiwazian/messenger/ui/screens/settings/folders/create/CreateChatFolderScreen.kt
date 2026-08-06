/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.folders.create

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.result.ResultEffect
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.components.FramelessTextBox
import com.aiwazian.messenger.ui.components.ProfileCard
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionHeader
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import com.aiwazian.messenger.ui.screens.settings.folders.FolderChatsSelection
import com.aiwazian.messenger.ui.screens.settings.folders.icon
import com.aiwazian.messenger.ui.screens.settings.folders.titleText
import kotlinx.coroutines.flow.collectLatest

@Composable
fun CreateChatFolderScreen(viewModel: CreateChatFolderViewModel = hiltViewModel()) {
    val navBackStack = LocalNavBackStack.current
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    
    ResultEffect<FolderChatsSelection> { selection ->
        viewModel.applySelection(selection)
    }
    
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collectLatest { sideEffect ->
            when (sideEffect) {
                is CreateChatFolderSideEffect.FolderCreated -> navBackStack.removeLastOrNull()
            }
        }
    }
    
    Scaffold(topBar = {
        PageTopBar(
            title = { Text(stringResource(R.string.new_folder)) },
            navigationIcon = NavigationIcon(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                onClick = navBackStack::removeLastOrNull
            ),
            actions = if (uiState.canSave) {
                listOf(
                    TopBarAction(
                        icon = Icons.Rounded.Check,
                        onClick = viewModel::createFolder
                    )
                )
            } else {
                emptyList()
            })
    }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {
            SectionContainer(header = {
                SectionHeader(stringResource(R.string.folder_name))
            }) {
                FramelessTextBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    placeholder = stringResource(R.string.folder_name),
                    value = uiState.name,
                    onValueChange = viewModel::onNameChange
                )
            }
            
            SectionContainer(header = {
                SectionHeader(stringResource(R.string.included_chats))
            }) {
                SectionItem(
                    leadingIcon = Icons.Rounded.Add,
                    headlineText = stringResource(R.string.add_chats),
                    onClick = {
                        navBackStack.add(
                            AppRoute.SelectFolderChats(
                                selectedChatIds = uiState.selectedChatIds,
                                selectedCategories = uiState.selectedCategories
                            )
                        )
                    })
                
                uiState.selectedCategories.forEach { category ->
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(category.titleText().asString()) },
                        leadingContent = {
                            CategoryAvatar(category.icon())
                        })
                }
                
                uiState.chats.forEach { chat ->
                    ProfileCard(
                        id = chat.id,
                        headlineText = chat.chatName.asString(),
                        avatarUri = chat.avatarUri
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryAvatar(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
