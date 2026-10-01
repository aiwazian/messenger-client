/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.comments

import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.app.AppDialog
import com.aiwazian.messenger.ui.app.AppScaffold
import com.aiwazian.messenger.ui.app.AppSnackbar
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionHeader
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.section.SectionToggleItem
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun ChannelCommentsSettingsScreen(
    channelId: Long,
    commentsEnabled: Boolean,
    commentsRestrictedToSubscribers: Boolean,
    viewModel: ChannelCommentsSettingsViewModel = hiltViewModel()
) {
    LaunchedEffect(channelId) {
        viewModel.init(channelId, commentsEnabled, commentsRestrictedToSubscribers)
    }

    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarJob by remember { mutableStateOf<Job?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is ChannelCommentsSettingsEffect.ShowSnackbar -> {
                    snackbarJob?.cancel()
                    snackbarJob = scope.launch {
                        snackbarHostState.showSnackbar(
                            message = effect.message.asString(context),
                            duration = SnackbarDuration.Short
                        )
                    }
                }

                ChannelCommentsSettingsEffect.NavigateToBack -> Unit
            }
        }
    }

    if (showDeleteAllDialog) {
        AppDialog(
            title = stringResource(R.string.delete_all_comments),
            onDismissRequest = { showDeleteAllDialog = false },
            content = { Text(stringResource(R.string.delete_all_comments_confirm)) },
            buttons = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(
                    onClick = {
                        showDeleteAllDialog = false
                        viewModel.deleteAllComments()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.delete))
                }
            }
        )
    }

    AppScaffold(
        topBar = {
            PageTopBar(
                title = {
                    Text(stringResource(R.string.comments))
                }
            )
        },
        snackbarHost = {
            AppSnackbar(snackbarHostState)
        },
        modifier = Modifier.imePadding()
    ) {
        SectionContainer {
            SectionToggleItem(
                text = stringResource(R.string.enable_comments),
                isChecked = uiState.commentsEnabled,
                enabled = !uiState.isChangingCommentsEnabled,
                onCheckedChange = { viewModel.changeCommentsEnabled(!uiState.commentsEnabled) }
            )
        }

        SectionContainer {
            SectionItem(
                headlineText = stringResource(R.string.delete_all_comments),
                contentColor = MaterialTheme.colorScheme.error,
                onClick = { showDeleteAllDialog = true }
            )
        }

        SectionContainer(header = {
            SectionHeader(title = stringResource(R.string.who_can_comment))
        }) {
            SectionToggleItem(
                text = stringResource(R.string.only_subscribers),
                isChecked = uiState.commentsRestrictedToSubscribers,
                enabled = !uiState.isChangingCommentsRestricted,
                onCheckedChange = {
                    viewModel.changeCommentsRestrictedToSubscribers(
                        !uiState.commentsRestrictedToSubscribers
                    )
                }
            )
        }
    }
}
