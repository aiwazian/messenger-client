/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.folders.chats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Checkbox
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
import androidx.navigation3.runtime.result.LocalResultEventBus
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.ChatFolderCategory
import com.aiwazian.messenger.ui.components.FramelessTextBox
import com.aiwazian.messenger.ui.components.ProfileCard
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import com.aiwazian.messenger.ui.screens.settings.folders.FolderChatsSelection
import com.aiwazian.messenger.ui.screens.settings.folders.icon
import com.aiwazian.messenger.ui.screens.settings.folders.titleText

@Composable
fun SelectFolderChatsScreen(
    selectedChatIds: List<Long> = emptyList(),
    selectedCategories: List<ChatFolderCategory> = emptyList(),
    viewModel: SelectFolderChatsViewModel = hiltViewModel()
) {
    val navBackStack = LocalNavBackStack.current
    val resultBus = LocalResultEventBus.current
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.restoreSelection(selectedChatIds, selectedCategories)
    }
    
    Scaffold(topBar = {
        PageTopBar(
            title = { Text(stringResource(R.string.add_chats)) },
            navigationIcon = NavigationIcon(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                onClick = navBackStack::removeLastOrNull
            ),
            actions = if (uiState.hasSelection) {
                listOf(
                    TopBarAction(
                        icon = Icons.Rounded.Check,
                        onClick = {
                            resultBus.sendResult<FolderChatsSelection>(
                                result = viewModel.buildSelection()
                            )
                            navBackStack.removeLastOrNull()
                        })
                )
            } else {
                emptyList()
            }
        )
    }) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            SectionContainer {
                FramelessTextBox(
                    placeholder = stringResource(R.string.search),
                    value = uiState.query,
                    onValueChange = viewModel::onQueryChange
                )
            }
            
            SectionContainer {
                uiState.visibleCategories.forEach { category ->
                    CategoryRow(
                        category = category,
                        isSelected = category in uiState.selectedCategories,
                        onClick = { viewModel.toggleCategory(category) })
                }
                
                uiState.chats.forEach { chat ->
                    ProfileCard(
                        id = chat.id,
                        headlineText = chat.chatName.asString(),
                        avatarUri = chat.avatarUri,
                        trailingContent = {
                            Checkbox(
                                checked = chat.id in uiState.selectedChatIds,
                                onCheckedChange = null,
                                modifier = Modifier.padding(vertical = 14.dp, horizontal = 4.dp)
                            )
                        },
                        onClick = { viewModel.toggleChat(chat.id) })
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: ChatFolderCategory, isSelected: Boolean, onClick: () -> Unit
) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
        content = { Text(category.titleText().asString()) },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        },
        trailingContent = {
            Checkbox(
                checked = isSelected,
                onCheckedChange = null,
                modifier = Modifier.padding(vertical = 14.dp, horizontal = 4.dp)
            )
        })
}
