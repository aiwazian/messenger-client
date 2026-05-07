/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.GroupType
import com.aiwazian.messenger.ui.components.CountdownTextButton
import com.aiwazian.messenger.ui.components.CustomDialog
import com.aiwazian.messenger.ui.components.CustomSnackbar
import com.aiwazian.messenger.ui.components.FramelessTextBox
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import kotlinx.coroutines.flow.collectLatest

@Composable
fun GroupSettingsScreen(
    groupId: Long, viewModel: GroupSettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val navBackStack = LocalNavBackStack.current
    
    LaunchedEffect(groupId) {
        viewModel.init(groupId)
    }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(Unit) {
        viewModel.uiEffect.collectLatest { effect ->
            when (effect) {
                is GroupSettingsUiEffect.NavigateBack -> navBackStack.removeLastOrNull()
                
                is GroupSettingsUiEffect.NavigateToMain -> {
                    navBackStack.clear()
                    navBackStack.add(AppRoute.Main)
                }
                
                is GroupSettingsUiEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message.asString(context))
                }
            }
        }
    }
    
    val uiState by viewModel.uiState.collectAsState()
    val group = uiState.group
    
    Scaffold(
        topBar = {
            PageTopBar(
                navigationIcon = NavigationIcon(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = navBackStack::removeLastOrNull
                ), actions = if (uiState.hasChanges) {
                    listOf(
                        TopBarAction(
                            icon = Icons.Rounded.Check, onClick = viewModel::save
                        )
                    )
                } else emptyList()
            )
        }, snackbarHost = {
            CustomSnackbar(snackbarHostState)
        }, modifier = Modifier.imePadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            SectionContainer {
                FramelessTextBox(
                    value = group.name,
                    onValueChange = viewModel::changeName,
                    placeholder = stringResource(R.string.group_name)
                )
                
                FramelessTextBox(
                    value = group.bio.orEmpty(),
                    onValueChange = viewModel::changeBio,
                    placeholder = stringResource(R.string.description)
                )
            }
            
            SectionContainer {
                SectionItem(
                    leadingIcon = Icons.Outlined.Lock,
                    headlineText = stringResource(R.string.group_type),
                    onClick = {
                        navBackStack.add(AppRoute.GroupTypeSettings(group.id))
                    },
                    trailingText = if (group.groupType == GroupType.PUBLIC) {
                        stringResource(R.string.public_group)
                    } else {
                        stringResource(R.string.private_group)
                    }
                )
                
                SectionItem(
                    leadingIcon = Icons.Rounded.Link,
                    headlineText = stringResource(R.string.invite_links),
                    onClick = { navBackStack.add(AppRoute.GroupInviteLinks(groupId = uiState.group.id)) })
            }
            
            SectionContainer {
                SectionItem(
                    leadingIcon = Icons.Rounded.People,
                    headlineText = stringResource(R.string.members),
                    trailingText = group.members.toString(),
                    onClick = {
                        navBackStack.add(AppRoute.GroupMembers(group.id))
                    })
                SectionItem(
                    leadingIcon = Icons.Rounded.Block,
                    headlineText = stringResource(R.string.removed_user),
                    onClick = {
                        navBackStack.add(AppRoute.GroupBlackList(group.id))
                    })
            }
            
            SectionContainer {
                SectionItem(
                    headlineText = stringResource(R.string.delete_group),
                    contentColor = MaterialTheme.colorScheme.error,
                    onClick = viewModel::showDeleteDialog
                )
            }
        }
        
        if (uiState.showDeleteDialog) {
            CustomDialog(
                title = stringResource(R.string.delete_group),
                onDismissRequest = viewModel::hideDeleteDialog,
                buttons = {
                    TextButton(onClick = viewModel::hideDeleteDialog) {
                        Text(stringResource(R.string.cancel))
                    }
                    
                    CountdownTextButton(
                        text = stringResource(R.string.delete_group),
                        seconds = 5,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        onClickWhileRunning = viewModel::vibrate,
                        onClickAfterFinish = viewModel::delete
                    )
                }) {
                Text("Вы точно хотите удалить группу для себя и всех участников?")
            }
        }
    }
}
