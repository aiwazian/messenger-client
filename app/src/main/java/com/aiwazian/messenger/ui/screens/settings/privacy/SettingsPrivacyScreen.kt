/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.privacy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.PrivacyLevel
import com.aiwazian.messenger.ui.app.AppBottomSheet
import com.aiwazian.messenger.ui.app.AppBottomSheetFooterActions
import com.aiwazian.messenger.ui.app.AppDialog
import com.aiwazian.messenger.ui.app.AppScaffold
import com.aiwazian.messenger.ui.app.AppSnackbar
import com.aiwazian.messenger.ui.components.CountdownTextButton
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionButton
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionHeader
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.utils.SessionEndResolution
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
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var snackbarJob by remember { mutableStateOf<Job?>(null) }
    
    LaunchedEffect(Unit) {
        viewModel.init()
    }
    
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is SettingsPrivacySideEffect.ShowSnackbar -> {
                    snackbarJob?.cancel()
                    
                    snackbarJob = scope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar(
                            message = effect.message.asString(context),
                            duration = SnackbarDuration.Long
                        )
                    }
                }
                
                SettingsPrivacySideEffect.NavigateToLogin -> {
                    SessionManager.getSessionEndCallback()
                        ?.invoke(SessionEndResolution.NoAccountsLeft)
                }
            }
        }
    }
    
    AppScaffold(topBar = {
        PageTopBar(
            title = { Text(stringResource(R.string.confidentiality)) },
        )
    }, snackbarHost = {
        AppSnackbar(snackbarHostState)
    }) {
        SectionContainer {
            SectionButton(
                headlineText = stringResource(R.string.blocked_users),
                trailingText = uiState.blockedUsersCount.toString(),
                onClick = {
                    navBackStack.add(AppRoute.BlockedUserList)
                }
            )
        }
        
        SectionContainer(header = {
            SectionHeader(stringResource(R.string.confidentiality))
        }) {
            SectionButton(
                headlineText = stringResource(R.string.bio),
                trailingText = if (uiState.privacy.bio == PrivacyLevel.EVERYBODY) {
                    stringResource(R.string.everybody)
                } else {
                    stringResource(R.string.nobody)
                },
                onClick = {
                    navBackStack.add(AppRoute.SettingsBio(uiState.privacy.bio))
                })
            
            SectionButton(
                headlineText = stringResource(R.string.profile_photos),
                trailingText = if (uiState.privacy.profilePhoto == PrivacyLevel.EVERYBODY) {
                    stringResource(R.string.everybody)
                } else {
                    stringResource(R.string.nobody)
                },
                onClick = {
                    navBackStack.add(AppRoute.SettingsPhoto(uiState.privacy.profilePhoto))
                })
            
            SectionButton(
                headlineText = stringResource(R.string.last_seen),
                trailingText = if (uiState.privacy.lastSeen == PrivacyLevel.EVERYBODY) {
                    stringResource(R.string.everybody)
                } else {
                    stringResource(R.string.nobody)
                },
                onClick = {
                    navBackStack.add(AppRoute.SettingsLastSeen(uiState.privacy.lastSeen))
                })
            
            SectionButton(
                headlineText = stringResource(R.string.date_of_birth),
                trailingText = if (uiState.privacy.dateOfBirth == PrivacyLevel.EVERYBODY) {
                    stringResource(R.string.everybody)
                } else {
                    stringResource(R.string.nobody)
                },
                onClick = {
                    navBackStack.add(AppRoute.SettingsDateOfBirth(uiState.privacy.dateOfBirth))
                })
            
            SectionButton(
                headlineText = stringResource(R.string.invites),
                trailingText = if (uiState.privacy.invites == PrivacyLevel.EVERYBODY) {
                    stringResource(R.string.everybody)
                } else {
                    stringResource(R.string.nobody)
                },
                onClick = {
                    navBackStack.add(AppRoute.SettingsInvites(uiState.privacy.invites))
                })
            
            SectionButton(
                headlineText = stringResource(R.string.message_forwarding),
                trailingText = if (uiState.privacy.forwardedProfile == PrivacyLevel.EVERYBODY) {
                    stringResource(R.string.everybody)
                } else {
                    stringResource(R.string.nobody)
                },
                onClick = {
                    navBackStack.add(AppRoute.SettingsForwardedProfile(uiState.privacy.forwardedProfile))
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
            
            SectionButton(
                headlineText = stringResource(R.string.if_away_for),
                trailingText = inactivityText,
                onClick = viewModel::showInactivityBottomSheet
            )
            
            SectionButton(
                headlineText = stringResource(R.string.delete_account),
                contentColor = MaterialTheme.colorScheme.error,
                onClick = viewModel::onDeleteAccountClick
            )
        }
    }
    
    if (uiState.showInactivityBottomSheet) {
        AppBottomSheet(
            onDismissRequest = viewModel::hideInactivityBottomSheet,
            stickyFooter = {
                AppBottomSheetFooterActions.PrimaryButton(
                    text = stringResource(R.string.cancel),
                    onClick = viewModel::hideInactivityBottomSheet
                )
            }) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                Text(
                    text = stringResource(R.string.delete_account_if_inactive_for),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                SectionContainer(contentPadding = PaddingValues.Zero) {
                    SectionButton(
                        headlineText = stringResource(R.string.inactive_1_month),
                        onClick = { viewModel.updateDeleteAfterDays(30) }
                    )
                    SectionButton(
                        headlineText = stringResource(R.string.inactive_3_months),
                        onClick = { viewModel.updateDeleteAfterDays(90) }
                    )
                    SectionButton(
                        headlineText = stringResource(R.string.inactive_6_months),
                        onClick = { viewModel.updateDeleteAfterDays(180) }
                    )
                    SectionButton(
                        headlineText = stringResource(R.string.inactive_12_months),
                        onClick = { viewModel.updateDeleteAfterDays(365) }
                    )
                }
            }
        }
    }
    
    if (uiState.showDeleteBottomSheet) {
        AppBottomSheet(
            onDismissRequest = viewModel::hideDeleteBottomSheet,
            stickyFooter = {
                AppBottomSheetFooterActions.Custom {
                    CountdownTextButton(
                        text = stringResource(R.string.delete_anyway),
                        seconds = 15,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        onClickWhileRunning = viewModel::vibrate,
                        onClickAfterFinish = {
                            viewModel.hideDeleteBottomSheet()
                            viewModel.showDeleteDialog()
                        }
                    )
                }
            }) { padding ->
            Row(
                modifier = Modifier.padding(padding),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
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
        }
    }
    
    if (uiState.showDeleteDialog) {
        AppDialog(
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
                    shape = CircleShape,
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
