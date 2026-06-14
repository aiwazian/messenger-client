/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.members

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuItemColors
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.User
import com.aiwazian.messenger.ui.components.CustomDialog
import com.aiwazian.messenger.ui.components.CustomDropdownMenu
import com.aiwazian.messenger.ui.components.CustomSnackbar
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.utils.DialogController

@Composable
fun GroupMembersScreen(
    groupId: Long,
    viewModel: GroupMembersViewModel = hiltViewModel()
) {
    val navBackStack = LocalNavBackStack.current
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    
    val kickDialogController = remember { DialogController() }
    val blockDialogController = remember { DialogController() }
    
    LaunchedEffect(groupId) {
        viewModel.init(groupId)
    }
    
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is GroupMembersSideEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message.asString(context))
                }
                
                GroupMembersSideEffect.ShowKickConfirmation -> {
                    kickDialogController.show()
                }
                
                GroupMembersSideEffect.ShowBlockConfirmation -> {
                    blockDialogController.show()
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            PageTopBar(
                title = { Text(stringResource(R.string.members)) },
                navigationIcon = NavigationIcon(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = navBackStack::removeLastOrNull
                )
            )
        },
        snackbarHost = {
            CustomSnackbar(snackbarHostState)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SectionContainer {
                SectionItem(
                    leadingIcon = Icons.Rounded.PersonAdd,
                    headlineText = stringResource(R.string.add_member),
                    contentColor = MaterialTheme.colorScheme.primary,
                    onClick = { navBackStack.add(AppRoute.AddMember(groupId)) }
                )
            }
            
            SectionContainer {
                LazyColumn {
                    items(state.members) { user ->
                        MemberItem(
                            user = user,
                            onKick = { viewModel.onKickClick(user.id) },
                            onBlock = { viewModel.onBlockClick(user.id) }
                        )
                    }
                }
            }
        }
    }
    
    if (kickDialogController.isVisible) {
        CustomDialog(
            title = stringResource(R.string.kick),
            onDismissRequest = kickDialogController::hide,
            buttons = {
                TextButton(onClick = kickDialogController::hide) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(
                    onClick = {
                        viewModel.confirmKick()
                        kickDialogController.hide()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.kick))
                }
            }
        ) {
            Text("Вы уверены, что хотите выгнать этого пользователя?")
        }
    }
    
    if (blockDialogController.isVisible) {
        CustomDialog(
            title = stringResource(R.string.block_user),
            onDismissRequest = blockDialogController::hide,
            buttons = {
                TextButton(onClick = blockDialogController::hide) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(
                    onClick = {
                        viewModel.confirmBlock()
                        blockDialogController.hide()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.block_user))
                }
            }
        ) {
            Text(stringResource(R.string.block_user_confirm_message))
        }
    }
}

@Composable
fun MemberItem(
    user: User,
    onKick: () -> Unit,
    onBlock: () -> Unit
) {
    val navBackStack = LocalNavBackStack.current
    
    var showMenu by remember { mutableStateOf(false) }
    
    SectionItem(
        headlineText = "${user.firstName} ${user.lastName.orEmpty()}".trim(),
        supportingText = user.username?.let { "@$it" },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = null)
                }
                CustomDropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.kick)) },
                        onClick = {
                            showMenu = false
                            onKick()
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.PersonRemove, null)
                        },
                        colors = MenuItemColors(
                            textColor = MaterialTheme.colorScheme.error,
                            leadingIconColor = MaterialTheme.colorScheme.error,
                            trailingIconColor = MaterialTheme.colorScheme.error,
                            disabledTextColor = Color.Unspecified,
                            disabledLeadingIconColor = Color.Unspecified,
                            disabledTrailingIconColor = Color.Unspecified
                        )
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.block_user)) },
                        onClick = {
                            showMenu = false
                            onBlock()
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.Block, null)
                        },
                        colors = MenuItemColors(
                            textColor = MaterialTheme.colorScheme.error,
                            leadingIconColor = MaterialTheme.colorScheme.error,
                            trailingIconColor = MaterialTheme.colorScheme.error,
                            disabledTextColor = Color.Unspecified,
                            disabledLeadingIconColor = Color.Unspecified,
                            disabledTrailingIconColor = Color.Unspecified
                        )
                    )
                }
            }
        },
        onClick = {
            navBackStack.add(AppRoute.Chat(user.id, "${user.firstName} ${user.lastName.orEmpty()}"))
        }
    )
}
