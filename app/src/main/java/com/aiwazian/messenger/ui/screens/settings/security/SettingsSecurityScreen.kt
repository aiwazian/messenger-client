/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.security

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
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.components.CustomSnackbar
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionDescription
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.utils.LottieAnimation
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSecurityScreen(viewModel: SettingsSecurityViewModel = hiltViewModel()) {
    val navBackStack = LocalNavBackStack.current
    val context = LocalContext.current
    
    val uiState by viewModel.uiState.collectAsState()
    
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var snackbarJob by remember { mutableStateOf<Job?>(null) }
    
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is SettingsSecuritySideEffect.ShowSnackbar -> {
                    snackbarJob?.cancel()
                    
                    snackbarJob = launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar(
                            message = effect.message.asString(context),
                            duration = SnackbarDuration.Long
                        )
                    }
                }
                
                SettingsSecuritySideEffect.NavigateToCloudPassword -> {
                    navBackStack.add(AppRoute.SettingsCloudPassword)
                }
                
                SettingsSecuritySideEffect.NavigateToLogin -> {
                    navBackStack.add(AppRoute.SettingsLogin)
                }
            }
        }
    }
    
    Scaffold(
        topBar = { TopBar() },
        snackbarHost = { CustomSnackbar(snackbarHostState) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .verticalScroll(scrollState)
        ) {
            SectionContainer(footer = {
                SectionDescription(
                    text = "Просмотреть список устройств, на которых Ваш аккаунт авторизован в ${
                        stringResource(R.string.app_name)
                    }."
                )
            }) {
                SectionItem(
                    leadingIcon = Icons.Rounded.PersonOutline,
                    headlineText = stringResource(R.string.login),
                    onClick = viewModel::onLoginClick
                )
                
                SectionItem(
                    leadingIcon = Icons.Rounded.Key,
                    headlineText = stringResource(R.string.cloud_password),
                    onClick = viewModel::onCloudPasswordClick
                )
                
                val passcodeEnabledText = if (uiState.passcodeEnabled) {
                    stringResource(R.string.on)
                } else {
                    stringResource(R.string.off)
                }
                
                SectionItem(
                    leadingIcon = Icons.Outlined.Lock,
                    headlineText = stringResource(R.string.passcode_lock),
                    trailingText = passcodeEnabledText,
                    onClick = {
                        if (uiState.passcodeEnabled) {
                            navBackStack.add(AppRoute.SettingsPasscode)
                        } else {
                            viewModel.showBottomSheet()
                        }
                    }
                )
                
                SectionItem(
                    leadingIcon = Icons.Rounded.Devices,
                    headlineText = stringResource(R.string.devices),
                    trailingText = uiState.deviceCount.toString(),
                    onClick = {
                        navBackStack.add(AppRoute.SettingsDevices)
                    }
                )
            }
            
            if (uiState.showPasscodeBottomSheet) {
                ModalBottomSheet(dragHandle = null, onDismissRequest = viewModel::hideBottomSheet) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        Text(stringResource(R.string.passcode_lock_description))
                        TextButton(
                            onClick = {
                                viewModel.hideBottomSheet()
                                navBackStack.add(AppRoute.SettingsPasscode)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(stringResource(R.string.enable_passcode))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar() {
    val navBackStack = LocalNavBackStack.current
    
    PageTopBar(
        title = { Text(stringResource(R.string.security)) },
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            onClick = navBackStack::removeLastOrNull
        )
    )
}
