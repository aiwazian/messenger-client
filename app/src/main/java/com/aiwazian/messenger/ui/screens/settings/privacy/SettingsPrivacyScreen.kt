/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.privacy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import com.aiwazian.messenger.enums.PrivacyLevel
import com.aiwazian.messenger.ui.components.CountdownTextButton
import com.aiwazian.messenger.ui.components.CustomDialog
import com.aiwazian.messenger.ui.components.CustomSnackbar
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionHeader
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.utils.SessionManager
import com.aiwazian.messenger.utils.UiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPrivacyScreen(viewModel: SettingsPrivacyViewModel = hiltViewModel()) {
    val navBackStack = LocalNavBackStack.current
    val context = LocalContext.current
    
    val uiState by viewModel.uiState.collectAsState()
    
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var snackbarJob by remember { mutableStateOf<Job?>(null) }
    
    LaunchedEffect(Unit) {
        viewModel.init()
    }
    
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is SettingsPrivacySideEffect.ShowSnackbar -> {
                    snackbarJob?.cancel()
                    
                    snackbarJob = launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar(
                            message = effect.message.asString(context),
                            duration = SnackbarDuration.Long
                        )
                    }
                }
                
                SettingsPrivacySideEffect.NavigateToLogin -> {
                    SessionManager.getUnauthorizedCallback()?.invoke()
                }
            }
        }
    }
    
    Scaffold(topBar = { TopBar() }, snackbarHost = {
        CustomSnackbar(snackbarHostState)
    }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .verticalScroll(scrollState)
        ) {
            SectionContainer(header = {
                SectionHeader(stringResource(R.string.confidentiality))
            }) {
                SectionItem(
                    headlineText = stringResource(R.string.bio),
                    trailingText = if (uiState.privacy.bio == PrivacyLevel.EVERYBODY) {
                        stringResource(R.string.everybody)
                    } else {
                        stringResource(R.string.nobody)
                    },
                    onClick = {
                        navBackStack.add(AppRoute.SettingsBio(uiState.privacy.bio))
                    })
                
                SectionItem(
                    headlineText = stringResource(R.string.profile_photos),
                    trailingText = if (uiState.privacy.profilePhoto == PrivacyLevel.EVERYBODY) {
                        stringResource(R.string.everybody)
                    } else {
                        stringResource(R.string.nobody)
                    },
                    onClick = {
                        navBackStack.add(AppRoute.SettingsPhoto(uiState.privacy.profilePhoto))
                    })
                
                SectionItem(
                    headlineText = stringResource(R.string.last_seen),
                    trailingText = if (uiState.privacy.lastSeen == PrivacyLevel.EVERYBODY) {
                        stringResource(R.string.everybody)
                    } else {
                        stringResource(R.string.nobody)
                    },
                    onClick = {
                        navBackStack.add(AppRoute.SettingsLastSeen(uiState.privacy.lastSeen))
                    })
                
                SectionItem(
                    headlineText = stringResource(R.string.date_of_birth),
                    trailingText = if (uiState.privacy.dateOfBirth == PrivacyLevel.EVERYBODY) {
                        stringResource(R.string.everybody)
                    } else {
                        stringResource(R.string.nobody)
                    },
                    onClick = {
                        navBackStack.add(AppRoute.SettingsDateOfBirth(uiState.privacy.dateOfBirth))
                    })
                
                SectionItem(
                    headlineText = stringResource(R.string.invites),
                    trailingText = if (uiState.privacy.invites == PrivacyLevel.EVERYBODY) {
                        stringResource(R.string.everybody)
                    } else {
                        stringResource(R.string.nobody)
                    },
                    onClick = {
                        navBackStack.add(AppRoute.SettingsInvites(uiState.privacy.invites))
                    })
            }
            
            SectionContainer(header = {
                SectionHeader(stringResource(R.string.delete_account))
            }) {
                val inactivityText = when (uiState.privacy.deleteAfterDays) {
                    30 -> UiText.StringResource(R.string.inactive_1_month)
                    90 -> UiText.StringResource(R.string.inactive_3_months)
                    180 -> UiText.StringResource(R.string.inactive_6_months)
                    365 -> UiText.StringResource(R.string.inactive_12_months)
                    else -> UiText.DynamicString("")
                }.asString()
                
                SectionItem(
                    headlineText = stringResource(R.string.if_away_for),
                    trailingText = inactivityText,
                    onClick = viewModel::showInactivityBottomSheet
                )
            }
            SectionContainer {
                SectionItem(
                    headlineText = stringResource(R.string.delete_account),
                    contentColor = MaterialTheme.colorScheme.error,
                    onClick = viewModel::onDeleteAccountClick
                )
            }
        }
    }
    
    if (uiState.showInactivityBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = viewModel::hideInactivityBottomSheet,
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.delete_account_if_inactive_for),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                Column {
                    SectionItem(
                        headlineText = stringResource(R.string.inactive_1_month),
                        onClick = { viewModel.updateDeleteAfterDays(30) }
                    )
                    SectionItem(
                        headlineText = stringResource(R.string.inactive_3_months),
                        onClick = { viewModel.updateDeleteAfterDays(90) }
                    )
                    SectionItem(
                        headlineText = stringResource(R.string.inactive_6_months),
                        onClick = { viewModel.updateDeleteAfterDays(180) }
                    )
                    SectionItem(
                        headlineText = stringResource(R.string.inactive_12_months),
                        onClick = { viewModel.updateDeleteAfterDays(365) }
                    )
                }
                TextButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    onClick = viewModel::hideInactivityBottomSheet,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
    
    if (uiState.showDeleteBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = viewModel::hideDeleteBottomSheet,
            dragHandle = null
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WarningAmber,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp)
                    )
                    Text(
                        text = stringResource(R.string.delete_account_message), lineHeight = 18.sp
                    )
                }
                
                CountdownTextButton(
                    text = "Все равно удалить",
                    seconds = 15,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    onClickWhileRunning = viewModel::vibrate,
                    onClickAfterFinish = {
                        viewModel.hideDeleteBottomSheet()
                        viewModel.showDeleteDialog()
                    }
                )
            }
        }
    }
    
    if (uiState.showDeleteDialog) {
        CustomDialog(
            title = stringResource(R.string.delete_account),
            onDismissRequest = viewModel::hideDeleteDialog,
            content = {
                Text(
                    text = stringResource(R.string.delete_account_confirm),
                    lineHeight = 18.sp
                )
            },
            buttons = {
                TextButton(onClick = viewModel::hideDeleteDialog) {
                    Text(stringResource(R.string.no))
                }
                
                CountdownTextButton(
                    text = stringResource(R.string.yes),
                    seconds = 15,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    onClickWhileRunning = viewModel::vibrate,
                    onClickAfterFinish = {
                        viewModel.hideDeleteDialog()
                        viewModel.deleteAccount()
                    }
                )
            })
    }
}

@Composable
private fun TopBar() {
    val navBackStack = LocalNavBackStack.current
    
    PageTopBar(
        title = { Text(stringResource(R.string.confidentiality)) }, navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Rounded.ArrowBack, onClick = navBackStack::removeLastOrNull
        )
    )
}
