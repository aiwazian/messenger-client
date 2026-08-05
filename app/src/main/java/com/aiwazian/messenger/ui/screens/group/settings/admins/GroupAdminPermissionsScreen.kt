/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.admins

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import com.aiwazian.messenger.ui.app.AppSnackbar
import com.aiwazian.messenger.ui.components.FramelessTextBox
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionDescription
import com.aiwazian.messenger.ui.components.section.SectionHeader
import com.aiwazian.messenger.ui.components.section.SectionToggleItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.components.topBar.TopBarAction

@Composable
fun GroupAdminPermissionsScreen(
    groupId: Long,
    userId: Long,
    viewModel: GroupAdminPermissionsViewModel = hiltViewModel()
) {
    val navBackStack = LocalNavBackStack.current
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(groupId, userId) {
        viewModel.init(groupId, userId)
    }
    
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                GroupAdminPermissionsSideEffect.NavigateBack -> navBackStack.removeLastOrNull()
                
                is GroupAdminPermissionsSideEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message.asString(context))
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            PageTopBar(
                title = { Text(stringResource(R.string.admin_permissions)) },
                navigationIcon = NavigationIcon(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = navBackStack::removeLastOrNull
                ),
                actions = if (uiState.isReadOnly) {
                    emptyList()
                } else {
                    listOf(
                        TopBarAction(
                            icon = Icons.Rounded.Check,
                            onClick = viewModel::save
                        )
                    )
                }
            )
        },
        snackbarHost = {
            AppSnackbar(snackbarHostState)
        },
        modifier = Modifier.imePadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            SectionContainer(header = {
                SectionHeader(title = stringResource(R.string.admin_permissions))
            }) {
                SectionToggleItem(
                    text = stringResource(R.string.invite_links),
                    supportingText = stringResource(R.string.permission_invite_links_description),
                    isChecked = uiState.canManageInviteLinks,
                    enabled = !uiState.isReadOnly,
                    onCheckedChange = viewModel::toggleManageInviteLinks
                )
                
                SectionToggleItem(
                    text = stringResource(R.string.edit_group_profile),
                    supportingText = stringResource(R.string.edit_group_profile_description),
                    isChecked = uiState.canEditProfile,
                    enabled = !uiState.isReadOnly,
                    onCheckedChange = viewModel::toggleEditProfile
                )
                
                SectionToggleItem(
                    text = stringResource(R.string.manage_admins),
                    supportingText = stringResource(R.string.manage_admins_description),
                    isChecked = uiState.canManageAdmins,
                    enabled = !uiState.isReadOnly,
                    onCheckedChange = viewModel::toggleManageAdmins
                )
            }
            
            if (!uiState.isReadOnly) {
                SectionContainer(
                    header = {
                        SectionHeader(title = stringResource(R.string.member_tag))
                    },
                    footer = {
                        SectionDescription(text = stringResource(R.string.member_tag_description))
                    }
                ) {
                    FramelessTextBox(
                        value = uiState.tag,
                        onValueChange = viewModel::changeTag,
                        placeholder = "${stringResource(R.string.member_tag)} (${stringResource(R.string.optional)})"
                    )
                }
            }
        }
    }
}
