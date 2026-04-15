/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.security

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionDescription
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar

@Composable
fun SettingsSecurityScreen(viewModel: SettingsSecurityViewModel = hiltViewModel()) {
    val navBackStack = LocalNavBackStack.current
    
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
                SectionContainer(footer = {
                    SectionDescription(
                        text = "Просмотреть список устройств, на которых Ваш аккаунт авторизован в ${
                            stringResource(R.string.app_name)
                        }."
                    )
                }) {
                    SectionItem(
                        leadingIcon = Icons.Rounded.Key,
                        headlineText = stringResource(R.string.cloud_password),
                        trailingText = stringResource(R.string.on),
                        onClick = {
                            navBackStack.add(AppRoute.SettingsCloudPassword)
                        }
                    )
                    
                    val passcodeEnabledText = if (passcodeEnabled) {
                        stringResource(R.string.on)
                    } else {
                        stringResource(R.string.off)
                    }
                    
                    SectionItem(
                        leadingIcon = Icons.Outlined.Lock,
                        headlineText = stringResource(R.string.passcode_lock),
                        trailingText = passcodeEnabledText,
                        onClick = {
                            navBackStack.add(AppRoute.SettingsPasscode)
                        }
                    )
                    
                    SectionItem(
                        leadingIcon = Icons.Rounded.Devices,
                        headlineText = stringResource(R.string.devices),
                        trailingText = deviceCount.toString(),
                        onClick = {
                            navBackStack.add(AppRoute.SettingsDevices)
                        }
                    )
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
