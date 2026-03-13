package com.aiwazian.messenger.ui.settings.security

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.data.NavigationIcon
import com.aiwazian.messenger.ui.element.PageTopBar
import com.aiwazian.messenger.ui.element.SectionContainer
import com.aiwazian.messenger.ui.element.SectionDescription
import com.aiwazian.messenger.ui.element.SectionItem
import com.aiwazian.messenger.viewModels.NavigationViewModel
import com.aiwazian.messenger.viewModels.SettingsSecurityViewModel

@Composable
fun SettingsSecurityScreen() {
    Content()
}

@Composable
private fun Content() {
    val navViewModel = viewModel<NavigationViewModel>()
    val viewModel = hiltViewModel<SettingsSecurityViewModel>()
    
    val deviceCount by viewModel.deviceCount.collectAsState()
    val passcodeEnabled by viewModel.isEnablePasscode.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.init()
    }
    
    val scrollState = rememberScrollState()
    
    Scaffold(
        topBar = { TopBar() }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Column {
                SectionContainer {
                    SectionItem(
                        icon = Icons.Outlined.Key,
                        text = stringResource(R.string.cloud_password),
                        primaryText = stringResource(R.string.on),
                        onClick = {
                            navViewModel.addScreenInStack {
                                SettingsCloudPasswordScreen()
                            }
                        }
                    )
                    
                    val passcodeEnabledText = if (passcodeEnabled) {
                        stringResource(R.string.on)
                    } else {
                        stringResource(R.string.off)
                    }
                    
                    SectionItem(
                        icon = Icons.Outlined.Lock,
                        text = stringResource(R.string.passcode_lock),
                        primaryText = passcodeEnabledText,
                        onClick = {
                            navViewModel.addScreenInStack {
                                SettingsPasscodeScreen(passcodeEnabled)
                            }
                        }
                    )
                    
                    SectionItem(
                        icon = Icons.Outlined.Devices,
                        text = stringResource(R.string.devices),
                        primaryText = deviceCount.toString(),
                        onClick = {
                            navViewModel.addScreenInStack {
                                SettingsDevicesScreen()
                            }
                        }
                    )
                }
                
                SectionDescription(
                    text = "Просмотреть список устройств, на которых Ваш аккаунт авторизован в ${
                        stringResource(R.string.app_name)
                    }."
                )
            }
        }
    }
}

@Composable
private fun TopBar() {
    val navViewModel = viewModel<NavigationViewModel>()
    
    PageTopBar(
        title = { Text(stringResource(R.string.security)) },
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            onClick = navViewModel::removeLastScreenInStack
        )
    )
}
