package com.aiwazian.messenger.ui.settings.security

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.data.NavigationIcon
import com.aiwazian.messenger.data.SessionInfo
import com.aiwazian.messenger.ui.element.CustomDialog
import com.aiwazian.messenger.ui.element.PageTopBar
import com.aiwazian.messenger.ui.element.SectionContainer
import com.aiwazian.messenger.ui.element.SectionHeader
import com.aiwazian.messenger.ui.element.SectionRadioItem
import com.aiwazian.messenger.viewModels.DevicesViewModel
import com.aiwazian.messenger.viewModels.NavigationViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsDevicesScreen() {
    Content()
}

@Composable
private fun Content() {
    val navViewModel = viewModel<NavigationViewModel>()
    val devicesViewModel = hiltViewModel<DevicesViewModel>()
    
    val currentSession by devicesViewModel.currentSession.collectAsState()
    
    val sessions by devicesViewModel.sessions.collectAsState()
    val bottomSheetDialog = devicesViewModel.sessionInfoDialog
    val terminateSessionDialog = devicesViewModel.terminateSessionDialog
    
    LaunchedEffect(Unit) {
        devicesViewModel.getSessions()
    }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    val scrollState = rememberScrollState()
    
    val scope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            PageTopBar(
                title = { Text(stringResource(R.string.devices)) },
                navigationIcon = NavigationIcon(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    onClick = navViewModel::removeLastScreenInStack
                )
            )
        },
        snackbarHost = {
            SwipeDismissSnackbarHost(snackbarHostState)
        }) {
        Column(
            Modifier
                .padding(it)
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            //            Column(
            //                modifier = Modifier
            //                    .fillMaxWidth()
            //                    .padding(vertical = 10.dp),
            //                verticalArrangement = Arrangement.spacedBy(10.dp),
            //                horizontalAlignment = Alignment.CenterHorizontally
            //            ) {
            //                val composition by rememberLottieComposition(
            //                    spec = LottieCompositionSpec.Asset(LottieAnimation.APPLE_PHONE)
            //                )
            //
            //                LottieAnimation(
            //                    composition = composition,
            //                    modifier = Modifier.size(100.dp),
            //                    iterations = LottieConstants.IterateForever,
            //                    isPlaying = true
            //                )
            //
            //                Text(
            //                    text = "Вы можете зайти в приложение с помощью QR-кода.",
            //                    fontSize = 14.sp,
            //                    lineHeight = 14.sp,
            //                    textAlign = TextAlign.Center,
            //                    color = MaterialTheme.colorScheme.onSurfaceVariant
            //                )
            //
            //                Button(
            //                    onClick = { },
            //                    modifier = Modifier
            //                        .fillMaxWidth()
            //                        .padding(10.dp),
            //                    shape = RoundedCornerShape(10.dp),
            //                ) {
            //                    Icon(
            //                        imageVector = Icons.Outlined.QrCode,
            //                        contentDescription = null,
            //                    )
            //                    Text(
            //                        text = stringResource(R.string.connect_device),
            //                        modifier = Modifier.padding(8.dp),
            //                    )
            //                }
            //            }
            
            SectionHeader(stringResource(R.string.this_device))
            
            SectionContainer {
                DeviceCard(
                    text = currentSession.deviceName,
                    onClick = {
                        devicesViewModel.openSession(currentSession.id)
                        bottomSheetDialog.show()
                    })
                
            }
            //
            //            if (sessions.isNotEmpty()) {
            //                SectionContainer {
            //                    SectionItem(
            //                        icon = Icons.Outlined.BackHand,
            //                        iconColor = MaterialTheme.colorScheme.error,
            //                        text = stringResource(R.string.terminate_all_other_sessions),
            //                        textColor = MaterialTheme.colorScheme.error,
            //                        colors = ButtonDefaults.textButtonColors(
            //                            contentColor = MaterialTheme.colorScheme.error
            //                        ),
            //                        onClick = {
            //                            devicesViewModel.setConfirmDialogAction {
            //                                devicesViewModel.terminateAllOtherSessions()
            //                            }
            //                            bottomSheetDialog.show()
            //                        })
            //                }
            //
            //                SectionDescription(text = stringResource(R.string.terminate_all_other_sessions_description))
            //            }
            
            if (sessions.isNotEmpty()) {
                SectionHeader(title = stringResource(R.string.active_sessions))
                
                SectionContainer {
                    sessions.forEach { session ->
                        DeviceCard(
                            text = session.deviceName,
                            onClick = {
                                devicesViewModel.openSession(session.id)
                                bottomSheetDialog.show()
                            })
                    }
                }
            }
        }
        
        if (terminateSessionDialog.isVisible) {
            TerminateSessionDialog(
                onDismiss = terminateSessionDialog::hide,
                onConfirm = {
                    devicesViewModel.getConfirmDialogAction()?.let { action ->
                        scope.launch {
                            action.invoke()
                        }
                    }
                    terminateSessionDialog.hide()
                    bottomSheetDialog.hide()
                })
        }
        
        val openSession by devicesViewModel.openedSession.collectAsState()
        
        if (bottomSheetDialog.isVisible) {
            BottomModal(
                sessionInfo = openSession,
                onDismissRequest = bottomSheetDialog::hide,
                onConfirm = {
                    devicesViewModel.setConfirmDialogAction {
                        devicesViewModel.terminateSession(openSession.id)
                    }
                    terminateSessionDialog.show()
                })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomModal(
    sessionInfo: SessionInfo,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = sessionInfo.deviceName,
                fontSize = 18.sp
            )
            
            Text(sessionInfo.createdAt)
            
            if (sessionInfo.id != 0) {
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = stringResource(R.string.terminate_session),
                        modifier = Modifier.padding(8.dp),
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SwipeDismissSnackbarHost(snackbarHostState: SnackbarHostState) {
    SnackbarHost(hostState = snackbarHostState) { data ->
        var dismissed by remember { mutableStateOf(false) }
        
        if (!dismissed) {
            val swipeToDismissBoxState = rememberSwipeToDismissBoxState(
                confirmValueChange = {
                    it == SwipeToDismissBoxValue.EndToStart || it == SwipeToDismissBoxValue.StartToEnd
                })
            
            SwipeToDismissBox(
                state = swipeToDismissBoxState,
                enableDismissFromEndToStart = true,
                enableDismissFromStartToEnd = true,
                backgroundContent = { }) {
                Snackbar(
                    modifier = Modifier
                        .padding(12.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    Text(data.visuals.message)
                }
            }
        }
    }
}

@Composable
private fun DeviceCard(
    text: String,
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
            Text(
                text = text,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

@Composable
private fun TerminateSessionDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    CustomDialog(
        title = stringResource(R.string.terminate_session),
        onDismissRequest = onDismiss,
        content = {
            Text("Вы точно хотите завершить сеанс?")
        },
        buttons = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.terminate))
            }
        })
}

@Composable
private fun AutoTerminateSessionDialog(
    onDismissRequest: () -> Unit,
) {
    var selectedOption by remember { mutableStateOf("Выберите пункт") }
    
    val options = listOf(
        "1 нед.",
        "1 мес.",
        "1 год",
    )
    
    CustomDialog(
        title = "Автозавершение сессий",
        onDismissRequest = onDismissRequest,
        content = {
            Column {
                options.forEach { option ->
                    Card(shape = RoundedCornerShape(10.dp)) {
                        SectionRadioItem(
                            text = option,
                            selected = option == selectedOption,
                            onClick = {
                                selectedOption = option
                            })
                    }
                }
            }
        },
        buttons = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        })
}
