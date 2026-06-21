/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.security.passcode

import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.components.CodeBlocks
import com.aiwazian.messenger.ui.components.CountdownTextButton
import com.aiwazian.messenger.ui.components.CustomDialog
import com.aiwazian.messenger.ui.components.CustomNumberBoard
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.section.SectionToggleItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.screens.settings.security.SettingsSecurityViewModel
import com.aiwazian.messenger.utils.LottieAnimation
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SettingsPasscodeScreen(viewModel: SettingsSecurityViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    
    var showLockScreen by remember { mutableStateOf(uiState.passcodeEnabled) }
    
    if (showLockScreen) {
        SettingsPasscodeLockScreen(onBackToCreate = { showLockScreen = false })
    } else {
        SettingsPasscodeCreateScreen(onCreated = { showLockScreen = true })
    }
}

@Composable
fun SettingsPasscodeCreateScreen(
    passcodeViewModel: PasscodeViewModel = hiltViewModel(),
    onCreated: () -> Unit = {}
) {
    val navBackStack = LocalNavBackStack.current
    
    val uiState by passcodeViewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        passcodeViewModel.uiEffect.collectLatest { effect ->
            when (effect) {
                PasscodeUiEffect.NavigateBack -> navBackStack.removeLastOrNull()
                PasscodeUiEffect.ShowLockScreen -> onCreated()
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopBar()
        },
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.padding(top = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(40.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Создание код-пароля",
                        fontSize = 24.sp
                    )
                    
                    Text(
                        text = "Введите 4 цифры, которые хотите использовать для разблокировки приложения.",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        lineHeight = 16.sp
                    )
                }
                
                CodeBlocks(
                    count = PasscodeViewModel.MAX_LENGTH_PASSCODE,
                    showInput = false,
                    code = uiState.passcode
                )
            }
            
            val boardButtons = listOf(
                listOf(
                    "1",
                    "2",
                    "3"
                ),
                listOf(
                    "4",
                    "5",
                    "6"
                ),
                listOf(
                    "7",
                    "8",
                    "9"
                ),
                listOf(
                    null,
                    "0",
                    Icons.AutoMirrored.Rounded.Backspace
                ),
            )
            
            CustomNumberBoard(
                value = uiState.passcode,
                buttons = boardButtons,
                onChange = passcodeViewModel::onPasscodeChanged
            )
        }
    }
}

@Composable
fun SettingsPasscodeChangeScreen(passcodeViewModel: PasscodeViewModel = hiltViewModel()) {
    val navBackStack = LocalNavBackStack.current
    
    val uiState by passcodeViewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        passcodeViewModel.uiEffect.collectLatest { effect ->
            when (effect) {
                PasscodeUiEffect.NavigateBack -> navBackStack.removeLastOrNull()
                PasscodeUiEffect.ShowLockScreen -> {}
            }
        }
    }
    
    Scaffold(
        topBar = {
            PageTopBar(
                navigationIcon = NavigationIcon(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = navBackStack::removeLastOrNull
                )
            )
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.padding(top = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(40.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    
                    Text(
                        text = "Введите новый код-пароль",
                        fontSize = 24.sp
                    )
                    
                    Text(
                        text = "Введите 4 цифры, которые хотите использовать для разблокировки приложения.",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        lineHeight = 16.sp
                    )
                }
                
                CodeBlocks(
                    count = PasscodeViewModel.MAX_LENGTH_PASSCODE,
                    code = uiState.passcode
                )
            }
            
            val boardButtons = listOf(
                listOf(
                    "1",
                    "2",
                    "3"
                ),
                listOf(
                    "4",
                    "5",
                    "6"
                ),
                listOf(
                    "7",
                    "8",
                    "9"
                ),
                listOf(
                    null,
                    "0",
                    Icons.AutoMirrored.Rounded.Backspace
                ),
            )
            
            CustomNumberBoard(
                value = uiState.passcode,
                buttons = boardButtons,
                onChange = passcodeViewModel::onPasscodeChanged
            )
        }
    }
}

@Composable
private fun TopBar() {
    val navBackStack = LocalNavBackStack.current
    PageTopBar(
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            onClick = navBackStack::removeLastOrNull
        )
    )
}

@Composable
private fun SettingsPasscodeLockScreen(
    passcodeViewModel: PasscodeViewModel = hiltViewModel(),
    onBackToCreate: () -> Unit = {}
) {
    val navBackStack = LocalNavBackStack.current
    
    val fingerprintEnabled by passcodeViewModel.fingerprintEnabled.collectAsState()
    
    val context = LocalContext.current
    val hasEnrolledFingerprints = remember {
        val biometricManager = BiometricManager.from(context)
        biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }
    
    val disablePasscodeDialog = passcodeViewModel.disablePasscodeDialog
    
    val scrollState = rememberScrollState()
    
    LaunchedEffect(Unit) {
        passcodeViewModel.uiEffect.collectLatest { effect ->
            when (effect) {
                PasscodeUiEffect.NavigateBack -> {
                    onBackToCreate()
                }
                
                PasscodeUiEffect.ShowLockScreen -> {}
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopBar()
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val composition by rememberLottieComposition(
                    spec = LottieCompositionSpec.Asset(LottieAnimation.KEY_LOCK)
                )
                
                LottieAnimation(
                    composition = composition,
                    modifier = Modifier.size(100.dp),
                    iterations = LottieConstants.IterateForever,
                    isPlaying = true
                )
                
                Text(
                    text = "Для блокировки и разблокировки приложения нажмите на значок замка над списком чатов.",
                    fontSize = 14.sp,
                    lineHeight = 14.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
            }
            
            if (hasEnrolledFingerprints) {
                SectionContainer {
                    SectionToggleItem(
                        text = stringResource(R.string.fingerprint_unlock),
                        isChecked = fingerprintEnabled,
                        onCheckedChange = passcodeViewModel::toggleFingerprint
                    )
                }
            }
            
            SectionContainer {
                SectionItem(
                    headlineText = stringResource(R.string.change_passcode),
                    onClick = {
                        navBackStack.add(AppRoute.SettingsPasscodeChange)
                    })
            }
            
            SectionContainer {
                SectionItem(
                    headlineText = stringResource(R.string.turn_passcode_off),
                    contentColor = MaterialTheme.colorScheme.error,
                    onClick = disablePasscodeDialog::show
                )
            }
            
            if (disablePasscodeDialog.isVisible) {
                DisablePasscodeDialog(
                    onDismiss = disablePasscodeDialog::hide,
                    onConfirm = {
                        passcodeViewModel.disablePasscode()
                        disablePasscodeDialog.hide()
                    },
                    vibrate = passcodeViewModel::vibrate
                )
            }
        }
    }
}

@Composable
private fun DisablePasscodeDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    vibrate: () -> Unit
) {
    CustomDialog(
        title = stringResource(R.string.turn_passcode_off),
        onDismissRequest = onDismiss,
        content = {
            Text(
                text = "Вы точно хотите отключить пароль?",
            )
        },
        buttons = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
            CountdownTextButton(
                text = stringResource(R.string.turn_off),
                seconds = 5,
                onClickWhileRunning = vibrate,
                onClickAfterFinish = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            )
        })
}
