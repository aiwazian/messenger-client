/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.notification.category

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.ChatFolderCategory
import com.aiwazian.messenger.ui.app.AppDialog
import com.aiwazian.messenger.ui.app.AppDropdownMenu
import com.aiwazian.messenger.ui.app.AppDropdownMenuItem
import com.aiwazian.messenger.ui.app.AppSnackbar
import com.aiwazian.messenger.ui.components.ProfileCard
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun NotificationCategoryScreen(
    category: ChatFolderCategory,
    viewModel: NotificationCategoryViewModel = hiltViewModel()
) {
    val navBackStack = LocalNavBackStack.current
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarJob by remember { mutableStateOf<Job?>(null) }
    
    LaunchedEffect(category) {
        viewModel.init(category)
    }
    
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is NotificationCategorySideEffect.ShowSnackbar -> {
                    snackbarJob?.cancel()
                    snackbarJob = launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar(
                            message = context.getString(effect.messageResId),
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            PageTopBar(
                title = { Text(stringResource(categoryTitleRes(category))) },
                navigationIcon = NavigationIcon(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = navBackStack::removeLastOrNull
                )
            )
        },
        snackbarHost = {
            AppSnackbar(snackbarHostState)
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularWavyProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        /*
                         * Кнопка добавления стоит выше списка и не прячется, когда исключений
                         * нет: пустой экран без неё оставляет пользователя ни с чем.
                         */
                        SectionContainer {
                            SectionItem(
                                headlineText = stringResource(R.string.notification_exception_add),
                                leadingIcon = Icons.Outlined.Add,
                                onClick = {
                                    navBackStack.add(AppRoute.SelectNotificationExceptionChat(category))
                                }
                            )
                        }
                        
                        if (uiState.exceptions.isEmpty()) {
                            Text(
                                text = stringResource(R.string.notification_exceptions_empty),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(top = 32.dp)
                            )
                        } else {
                            SectionContainer {
                                uiState.exceptions.forEach { item ->
                                    ExceptionCard(
                                        item = item,
                                        onConfigure = {
                                            navBackStack.add(AppRoute.SettingsNotificationException(item.chatId))
                                        },
                                        onToggle = { viewModel.toggleException(item) },
                                        onRemove = { viewModel.removeException(item.chatId) }
                                    )
                                }
                            }
                            
                            SectionContainer {
                                SectionItem(
                                    headlineText = stringResource(R.string.delete_all_exceptions),
                                    contentColor = MaterialTheme.colorScheme.error,
                                    onClick = viewModel::showDeleteAllDialog
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (uiState.showDeleteAllDialog) {
        AppDialog(
            title = stringResource(R.string.delete_all_exceptions),
            onDismissRequest = viewModel::hideDeleteAllDialog,
            buttons = {
                TextButton(onClick = viewModel::hideDeleteAllDialog) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(onClick = viewModel::removeAllExceptions) {
                    Text(stringResource(R.string.delete))
                }
            },
            content = {
                Text(
                    text = stringResource(R.string.delete_all_exceptions_confirm),
                    lineHeight = 18.sp
                )
            }
        )
    }
}

@Composable
private fun ExceptionCard(
    item: NotificationExceptionItem,
    onConfigure: () -> Unit,
    onToggle: () -> Unit,
    onRemove: () -> Unit
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    
    val chat = item.chat
    val name = if (chat != null) chat.chatName.asString() else ""
    
    val supportingText = if (item.enabled) {
        stringResource(R.string.notification_exception_enabled)
    } else {
        stringResource(R.string.notification_exception_disabled)
    }
    
    Box {
        ProfileCard(
            id = item.chatId,
            headlineText = name,
            avatarUri = chat?.avatarUri,
            supportingText = supportingText,
            onClick = { isMenuExpanded = true }
        )
        
        AppDropdownMenu(
            expanded = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false }
        ) {
            AppDropdownMenuItem(
                text = { Text(stringResource(R.string.notification_exception_configure)) },
                onClick = {
                    isMenuExpanded = false
                    onConfigure()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = null
                    )
                }
            )
            
            /*
             * Подпись говорит о действии, а не о текущем состоянии: если уведомления
             * включены, предлагаем их выключить.
             */
            AppDropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (item.enabled) {
                                R.string.chat_disable_notifications
                            } else {
                                R.string.chat_enable_notifications
                            }
                        )
                    )
                },
                onClick = {
                    isMenuExpanded = false
                    onToggle()
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (item.enabled) {
                            Icons.Outlined.NotificationsOff
                        } else {
                            Icons.Outlined.Notifications
                        },
                        contentDescription = null
                    )
                }
            )
            
            AppDropdownMenuItem(
                text = { Text(stringResource(R.string.delete_exception)) },
                onClick = {
                    isMenuExpanded = false
                    onRemove()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null
                    )
                },
                contentColor = MaterialTheme.colorScheme.error
            )
        }
    }
}

private fun categoryTitleRes(category: ChatFolderCategory): Int {
    return when (category) {
        ChatFolderCategory.PRIVATE_CHATS -> R.string.private_chats
        ChatFolderCategory.GROUPS -> R.string.groups
        ChatFolderCategory.CHANNELS -> R.string.channels
    }
}
