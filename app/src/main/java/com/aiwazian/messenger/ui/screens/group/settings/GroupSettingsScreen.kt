/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PersonAddAlt1
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
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.GroupType
import com.aiwazian.messenger.ui.app.AppDialog
import com.aiwazian.messenger.ui.app.AppSnackbar
import com.aiwazian.messenger.ui.components.CountdownTextButton
import com.aiwazian.messenger.ui.components.FramelessTextBox
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionHeader
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import com.aiwazian.messenger.ui.screens.settings.profile.AvatarCropScreen
import com.aiwazian.messenger.ui.screens.settings.profile.SettingsProfileImageCarousel
import kotlinx.coroutines.flow.collectLatest
import java.io.File
import java.io.FileOutputStream

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
            AppSnackbar(snackbarHostState)
        }, modifier = Modifier.imePadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Box(modifier = Modifier.padding(start = 10.dp)) {
                SectionHeader(title = stringResource(R.string.profile_photos))
            }
            
            SettingsProfileImageCarousel(
                avatars = uiState.group.avatars,
                onAddPhoto = viewModel::setPendingAvatarUri,
                onDeletePhoto = viewModel::deleteAvatar
            )
            
            SectionContainer {
                FramelessTextBox(
                    value = uiState.group.name,
                    onValueChange = viewModel::changeName,
                    placeholder = stringResource(R.string.group_name)
                )
                
                FramelessTextBox(
                    value = uiState.group.bio.orEmpty(),
                    onValueChange = viewModel::changeBio,
                    placeholder = "${stringResource(R.string.description)} (${stringResource(R.string.optional)})",
                    singleLine = false
                )
            }
            
            SectionContainer {
                SectionItem(
                    leadingIcon = Icons.Outlined.Lock,
                    headlineText = stringResource(R.string.group_type),
                    onClick = {
                        navBackStack.add(AppRoute.GroupTypeSettings(uiState.group.id))
                    },
                    trailingText = if (uiState.group.groupType == GroupType.PUBLIC) {
                        stringResource(R.string.public_group)
                    } else {
                        stringResource(R.string.private_group)
                    }
                )
                
                SectionItem(
                    leadingIcon = Icons.Rounded.Link,
                    headlineText = stringResource(R.string.invite_links),
                    onClick = {
                        navBackStack.add(AppRoute.GroupInviteLinks(groupId = uiState.group.id))
                    }
                )
            }
            
            SectionContainer {
                SectionItem(
                    leadingIcon = Icons.Rounded.People,
                    headlineText = stringResource(R.string.members),
                    trailingText = uiState.group.members.toString(),
                    onClick = {
                        navBackStack.add(AppRoute.GroupMembers(uiState.group.id))
                    })
                SectionItem(
                    leadingIcon = Icons.Rounded.AdminPanelSettings,
                    headlineText = stringResource(R.string.administrators),
                    onClick = {
                        navBackStack.add(AppRoute.GroupAdmins(groupId = uiState.group.id))
                    }
                )
                SectionItem(
                    leadingIcon = Icons.Rounded.PersonAddAlt1,
                    headlineText = stringResource(R.string.join_requests),
                    onClick = {
                        navBackStack.add(AppRoute.GroupJoinRequests(groupId = uiState.group.id))
                    }
                )
                SectionItem(
                    leadingIcon = Icons.Rounded.Block,
                    headlineText = stringResource(R.string.removed_user),
                    trailingText = uiState.group.removedUsers.toString(),
                    onClick = {
                        navBackStack.add(AppRoute.GroupBlackList(uiState.group.id))
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
            AppDialog(
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
    
    if (uiState.pendingAvatarUri != null) {
        val context = LocalContext.current
        AvatarCropScreen(
            imageUri = uiState.pendingAvatarUri!!, onCropConfirmed = { bitmap ->
                val file = File(context.cacheDir, "avatar_${System.currentTimeMillis()}.png")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                
                val contentUri = FileProvider.getUriForFile(
                    context, "${context.packageName}.fileprovider", file
                )
                
                viewModel.uploadAvatar(contentUri)
                viewModel.clearPendingAvatarUri()
            }, onDismiss = viewModel::clearPendingAvatarUri
        )
    }
}
