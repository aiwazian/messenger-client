/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.GroupType
import com.aiwazian.messenger.ui.components.CustomDialog
import com.aiwazian.messenger.ui.components.FramelessTextBox
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavHost
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import com.aiwazian.messenger.utils.DialogController
import kotlinx.coroutines.flow.collectLatest

@Composable
fun GroupSettingsScreen(
    groupId: Long,
    groupViewModel: GroupViewModel = hiltViewModel()
) {
    val navHost = LocalNavHost.current
    
    LaunchedEffect(groupId) {
        if (groupId != -1L) {
            groupViewModel.init(groupId)
        }
    }
    
    LaunchedEffect(Unit) {
        groupViewModel.updateEffect.collectLatest { effect ->
            when (effect) {
                is UpdateGroupEffect.NavigateBack -> navHost.removeLastOrNull()
                
                is UpdateGroupEffect.NavigateToMain -> {
                    navHost.clear()
                    navHost.add(AppRoute.Main)
                }
            }
        }
    }
    
    val group by groupViewModel.group.collectAsState()
    val updateState by groupViewModel.updateState.collectAsState()
    
    val deleteGroupDialog = DialogController()
    val isLoading = updateState is UpdateGroupState.Loading
    
    Scaffold(topBar = {
        PageTopBar(
            navigationIcon = NavigationIcon(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                onClick = navHost::removeLastOrNull
            ),
            actions = listOf(
                TopBarAction(
                    icon = Icons.Rounded.Check,
                    onClick = groupViewModel::saveGroup
                )
            )
        )
    }) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            SectionContainer {
                FramelessTextBox(
                    value = group.name,
                    onValueChange = groupViewModel::changeGroupName,
                    placeholder = stringResource(R.string.group_name)
                )
                
                FramelessTextBox(
                    value = group.bio.orEmpty(),
                    onValueChange = groupViewModel::changeGroupBio,
                    placeholder = stringResource(R.string.description)
                )
            }
            
            SectionContainer {
                SectionItem(
                    leadingIcon = Icons.Outlined.Lock,
                    headlineText = stringResource(R.string.group_type),
                    onClick = {
                        navHost.add(AppRoute.GroupTypeSettings(group.id))
                    },
                    trailingText = if (group.groupType == GroupType.PUBLIC) {
                        "Публичная группа"
                    } else {
                        "Приватная группа"
                    }
                )
            }
            
            SectionContainer {
                SectionItem(
                    leadingIcon = Icons.Rounded.People,
                    headlineText = stringResource(R.string.members),
                    trailingText = group.members.toString(),
                    onClick = {
                        navHost.add(AppRoute.GroupMembers(group.id))
                    }
                )
                SectionItem(
                    leadingIcon = Icons.Rounded.Block,
                    headlineText = stringResource(R.string.removed_user),
                    onClick = {
                        navHost.add(AppRoute.GroupBlackList(group.id))
                    }
                )
            }
            
            SectionContainer {
                SectionItem(
                    headlineText = stringResource(R.string.delete_group),
                    contentColor = MaterialTheme.colorScheme.error,
                    onClick = deleteGroupDialog::show
                )
            }
        }
        
        if (deleteGroupDialog.isVisible) {
            CustomDialog(
                title = stringResource(R.string.delete_group),
                onDismissRequest = deleteGroupDialog::hide,
                buttons = {
                    TextButton(onClick = deleteGroupDialog::hide) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(
                        onClick = {
                            groupViewModel.deleteGroup()
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.delete_group))
                    }
                }
            ) {
                Text("Вы точно хотите удалить группу для себя и всех участников?")
            }
        }
    }
}
