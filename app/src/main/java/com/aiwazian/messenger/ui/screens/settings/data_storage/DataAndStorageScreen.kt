/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.data_storage

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.app.AppDialog
import com.aiwazian.messenger.ui.app.AppScaffold
import com.aiwazian.messenger.ui.app.AppSnackbar
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.PageTopBar

@Composable
fun DataAndStorageScreen(
    viewModel: DataAndStorageViewModel = hiltViewModel()
) {
    val navBackStack = LocalNavBackStack.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is DataAndStorageUiEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message.asString(context))
                }
            }
        }
    }
    
    if (uiState.showClearDraftsDialog) {
        AppDialog(
            title = stringResource(R.string.clear_drafts),
            onDismissRequest = viewModel::hideClearDraftsDialog,
            buttons = {
                TextButton(onClick = viewModel::hideClearDraftsDialog) {
                    Text(text = stringResource(R.string.cancel))
                }
                TextButton(
                    onClick = {
                        viewModel.clearAllDrafts()
                        viewModel.hideClearDraftsDialog()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(text = stringResource(R.string.delete))
                }
            }
        ) {
            Text(text = stringResource(R.string.clear_drafts_confirm_message))
        }
    }
    
    AppScaffold(
        topBar = {
            PageTopBar(
                title = {
                    Text(text = stringResource(R.string.data_and_storage))
                }
            )
        },
        snackbarHost = {
            AppSnackbar(hostState = snackbarHostState)
        }
    ) {
        SectionContainer {
            SectionItem(
                headlineText = stringResource(R.string.storage_usage),
                onClick = {
                    navBackStack.add(AppRoute.SettingsStorage)
                }
            )
        }
        
        SectionContainer {
            SectionItem(
                headlineText = stringResource(R.string.automatic_media_download),
                onClick = {
                    navBackStack.add(AppRoute.SettingsAutoDownloadMedia)
                }
            )
        }
        
        SectionContainer {
            SectionItem(
                headlineText = stringResource(R.string.clear_drafts),
                onClick = viewModel::showClearDraftsDialog
            )
        }
    }
}
