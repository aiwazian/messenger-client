/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.security.passcode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.aiwazian.messenger.ui.components.AnimatedIntroScreen
import com.aiwazian.messenger.ui.components.CodeBlocks
import com.aiwazian.messenger.ui.components.CustomDialog
import com.aiwazian.messenger.ui.components.CustomNumberBoard
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavHost
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.screens.settings.security.SettingsSecurityViewModel
import com.aiwazian.messenger.utils.LottieAnimation
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SettingsPasscodeScreen() {
    val securityViewModel = hiltViewModel<SettingsSecurityViewModel>()
    val passcodeEnabled by securityViewModel.isEnablePasscode.collectAsState()
    
    if (passcodeEnabled) {
        SettingsPasscodeLockScreen()
    } else {
        PasscodeLockMainScreen()
    }
}

@Composable
fun SettingsPasscodeCreateScreen(
    passcodeViewModel: PasscodeViewModel = hiltViewModel()
) {
    val navHost = LocalNavHost.current
    
    val uiState by passcodeViewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        passcodeViewModel.uiEffect.collectLatest { effect ->
            when (effect) {
                PasscodeUiEffect.NavigateBack -> navHost.removeLastOrNull()
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
                onChange = passcodeViewModel::onPasscodeChanged,
                onVibrate = passcodeViewModel::vibrate
            )
        }
    }
}

@Composable
fun SettingsPasscodeChangeScreen(
    passcodeViewModel: PasscodeViewModel = hiltViewModel()
) {
    val navHost = LocalNavHost.current
    
    val uiState by passcodeViewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        passcodeViewModel.uiEffect.collectLatest { effect ->
            when (effect) {
                PasscodeUiEffect.NavigateBack -> navHost.removeLastOrNull()
            }
        }
    }
    
    Scaffold(
        topBar = {
            PageTopBar(
                navigationIcon = NavigationIcon(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = navHost::removeLastOrNull
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
                onChange = passcodeViewModel::onPasscodeChanged,
                onVibrate = passcodeViewModel::vibrate
            )
        }
    }
}

@Composable
private fun PasscodeLockMainScreen() {
    val navHost = LocalNavHost.current
    
    Scaffold(
        topBar = {
            TopBarMain()
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.padding(innerPadding)
        ) {
            AnimatedIntroScreen(
                animation = LottieAnimation.KEY_LOCK,
                title = stringResource(R.string.passcode_lock),
                description = stringResource(R.string.passcode_lock_description),
                buttonText = stringResource(R.string.enable_passcode),
                buttonClick = {
                    navHost.add(AppRoute.SettingsPasscodeCreate)
                })
        }
    }
}

@Composable
private fun TopBar() {
    val navHost = LocalNavHost.current
    PageTopBar(
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            onClick = navHost::removeLastOrNull
        )
    )
}

@Composable
private fun SettingsPasscodeLockScreen(
    passcodeViewModel: PasscodeViewModel = hiltViewModel()
) {
    val navHost = LocalNavHost.current
    
    val disablePasscodeDialog = passcodeViewModel.disablePasscodeDialog
    
    val scrollState = rememberScrollState()
    
    LaunchedEffect(Unit) {
        passcodeViewModel.uiEffect.collectLatest { effect ->
            when (effect) {
                PasscodeUiEffect.NavigateBack -> navHost.removeLastOrNull()
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
            
            SectionContainer {
                SectionItem(
                    text = stringResource(R.string.change_passcode),
                    onClick = {
                        navHost.add(AppRoute.SettingsPasscodeChange)
                    })
            }
            
            SectionContainer {
                SectionItem(
                    text = stringResource(R.string.turn_passcode_off),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    onClick = disablePasscodeDialog::show
                )
            }
            
            if (disablePasscodeDialog.isVisible) {
                DisablePasscodeDialog(
                    onDismiss = disablePasscodeDialog::hide,
                    onConfirm = {
                        passcodeViewModel.disablePasscode()
                        disablePasscodeDialog.hide()
                    })
            }
        }
    }
}

@Composable
private fun DisablePasscodeDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
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
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.turn_off))
            }
        })
}

@Composable
private fun TopBarMain() {
    val navHost = LocalNavHost.current
    PageTopBar(
        title = { Text(stringResource(R.string.passcode_lock)) },
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            onClick = navHost::removeLastOrNull
        )
    )
}
