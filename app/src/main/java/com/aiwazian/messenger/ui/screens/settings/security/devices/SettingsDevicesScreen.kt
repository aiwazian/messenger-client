/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.security.devices

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.BackHand
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.Session
import com.aiwazian.messenger.extensions.toInstance
import com.aiwazian.messenger.extensions.toPrettyDateTime
import com.aiwazian.messenger.ui.components.CountdownTextButton
import com.aiwazian.messenger.ui.components.CustomBottomSheet
import com.aiwazian.messenger.ui.components.CustomDialog
import com.aiwazian.messenger.ui.components.CustomSnackbar
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionDescription
import com.aiwazian.messenger.ui.components.section.SectionHeader
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun SettingsDevicesScreen(viewModel: DevicesViewModel = hiltViewModel()) {
    val navBackStack = LocalNavBackStack.current
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var snackbarJob by remember { mutableStateOf<Job?>(null) }
    
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is DevicesSideEffect.ShowSnackbar -> {
                    snackbarJob?.cancel()
                    
                    snackbarJob = launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar(
                            message = effect.message.asString(context),
                            duration = SnackbarDuration.Long
                        )
                    }
                }
            }
        }
    }
    
    val currentSession = uiState.sessions.find { it.isCurrent }
    val otherSessions = uiState.sessions.filter { !it.isCurrent }
    
    Scaffold(
        topBar = {
            PageTopBar(
                title = { Text(stringResource(R.string.devices)) },
                navigationIcon = NavigationIcon(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = navBackStack::removeLastOrNull
                )
            )
        },
        snackbarHost = {
            CustomSnackbar(snackbarHostState)
        }) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            
            if (currentSession != null) {
                SectionContainer(header = {
                    SectionHeader(stringResource(R.string.this_device))
                }, footer = {
                    if (otherSessions.isNotEmpty()) {
                        SectionDescription(text = stringResource(R.string.terminate_all_other_sessions_description))
                    }
                }) {
                    DeviceCard(
                        session = currentSession,
                        onClick = { viewModel.openSession(currentSession) }
                    )
                    AnimatedVisibility(otherSessions.isNotEmpty()) {
                        SectionItem(
                            leadingIcon = Icons.Outlined.BackHand,
                            headlineText = stringResource(R.string.terminate_all_other_sessions),
                            contentColor = MaterialTheme.colorScheme.error,
                            onClick = viewModel::showTerminateAllOtherSessionsDialog
                        )
                    }
                }
            }
            
            if (otherSessions.isNotEmpty()) {
                SectionContainer(header = {
                    SectionHeader(title = stringResource(R.string.active_sessions))
                }, footer = {
                    SectionDescription(text = stringResource(R.string.sessions_android_only_message))
                }) {
                    otherSessions.forEach { session ->
                        DeviceCard(
                            session = session,
                            onClick = { viewModel.openSession(session) }
                        )
                    }
                }
            }
        }
        
        if (uiState.showTerminateSessionDialog) {
            TerminateSessionDialog(
                title = stringResource(R.string.terminate_session),
                message = stringResource(R.string.terminate_session_confirm_message),
                onDismiss = viewModel::hideTerminateSessionDialog,
                onConfirm = viewModel::terminateSession,
                vibrate = viewModel::vibrate
            )
        }
        
        if (uiState.showTerminateAllOtherSessionsDialog) {
            TerminateSessionDialog(
                title = stringResource(R.string.terminate_all_other_sessions),
                message = stringResource(R.string.terminate_all_other_sessions_confirm_message),
                onDismiss = viewModel::hideTerminateAllOtherSessionsDialog,
                onConfirm = viewModel::terminateAllOtherSessions,
                vibrate = viewModel::vibrate
            )
        }
        
        if (uiState.showSessionInfoBottomSheet && uiState.openedSession != null) {
            SessionInfoBottomSheet(
                session = uiState.openedSession!!,
                onDismissRequest = viewModel::closeSessionInfo,
                onTerminateClick = viewModel::showTerminateSessionDialog
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionInfoBottomSheet(
    session: Session,
    onDismissRequest: () -> Unit,
    onTerminateClick: () -> Unit
) {
    CustomBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = session.deviceModel,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoRow(
                    icon = Icons.Outlined.PhoneAndroid,
                    value = "${session.osName} ${session.osVersion}"
                )
                InfoRow(
                    icon = Icons.Outlined.DateRange,
                    value = session.createdAt.toInstance().toPrettyDateTime()
                )
            }
            
            if (!session.isCurrent) {
                TextButton(
                    onClick = onTerminateClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = stringResource(R.string.terminate_session),
                        modifier = Modifier.padding(4.dp),
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DeviceCard(
    session: Session,
    onClick: () -> Unit
) {
    Card(
        shape = RectangleShape,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_background),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(
                    text = session.deviceModel,
                    fontWeight = if (session.isCurrent) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = "${session.osName} ${session.osVersion}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TerminateSessionDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    vibrate: () -> Unit
) {
    CustomDialog(
        title = title,
        onDismissRequest = onDismiss,
        content = {
            Text(message)
        },
        buttons = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
            CountdownTextButton(
                text = stringResource(R.string.terminate),
                seconds = 5,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                onClickAfterFinish = onConfirm,
                onClickWhileRunning = vibrate
            )
        })
}
