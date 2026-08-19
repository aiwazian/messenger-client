/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.notification.exception

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.app.AppSnackbar
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.section.SectionToggleItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Настройка уведомлений конкретного чата.
 *
 * Открывается и из списка исключений, и при добавлении нового из списка чатов:
 * экран один, различается только строка удаления.
 */
@Composable
fun NotificationExceptionScreen(
    chatId: Long,
    viewModel: NotificationExceptionViewModel = hiltViewModel()
) {
    val navBackStack = LocalNavBackStack.current
    val context = LocalContext.current
    
    val uiState by viewModel.uiState.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarJob by remember { mutableStateOf<Job?>(null) }
    
    LaunchedEffect(chatId) {
        viewModel.init(chatId)
    }
    
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                NotificationExceptionSideEffect.NavigateBack -> {
                    navBackStack.removeLastOrNull()
                }
                
                is NotificationExceptionSideEffect.ShowSnackbar -> {
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
    
    val chat = uiState.chat
    val title = if (chat != null) chat.chatName.asString() else ""
    
    Scaffold(
        topBar = {
            PageTopBar(
                title = { Text(title) },
                navigationIcon = NavigationIcon(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = navBackStack::removeLastOrNull
                )
            )
        },
        snackbarHost = {
            AppSnackbar(snackbarHostState)
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            SectionContainer {
                SectionToggleItem(
                    text = stringResource(R.string.notification_exception_receive),
                    isChecked = uiState.notificationsEnabled,
                    onCheckedChange = viewModel::toggleNotifications
                )
            }
            
            /*
             * Исключения ещё нет — удалять нечего: экран открыт из списка чатов,
             * и строка появится сама, как только переключатель создаст исключение.
             */
            if (uiState.hasException) {
                SectionContainer {
                    SectionItem(
                        headlineText = stringResource(R.string.delete_exception),
                        contentColor = MaterialTheme.colorScheme.error,
                        onClick = viewModel::removeException
                    )
                }
            }
        }
    }
}
