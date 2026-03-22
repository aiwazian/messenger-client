/*
 * Copyright (c) 2026. Aiwazian.
 */

@file:OptIn(ExperimentalMaterial3Api::class)

package com.aiwazian.messenger.ui.screens.settings.security

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.AnimatedIntroScreen
import com.aiwazian.messenger.ui.components.navigation.LocalNavHost
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.utils.LottieAnimation

@Composable
fun SettingsCloudPasswordScreen(enabled: Boolean = true) {
    if (enabled) {
        Settings()
    } else {
        Main()
    }
}

@Composable
private fun Main() {
    val navHost = LocalNavHost.current
    Scaffold(topBar = {
        PageTopBar(
            navigationIcon = NavigationIcon(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                onClick = navHost::removeLastOrNull
            )
        )
    }) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            AnimatedIntroScreen(
                animation = LottieAnimation.KEY_LOCK,
                title = stringResource(R.string.cloud_password),
                description = stringResource(R.string.cloud_password_description),
                buttonText = "Включить",
                buttonClick = {
                    navHost.add(AppRoute.SettingsChangeCloudPassword)
                }
            )
        }
    }
}

@Composable
private fun Settings() {
    val navHost = LocalNavHost.current
    Scaffold(
        topBar = {
            TopBar()
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            SectionContainer {
                SectionItem(
                    "Сменить пароль",
                    onClick = {
                        navHost.add(AppRoute.SettingsChangeCloudPassword)
                    })
            }
        }
    }
}

@Composable
private fun TopBar() {
    val navHost = LocalNavHost.current
    PageTopBar(
        title = {
            Text(stringResource(R.string.cloud_password))
        },
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            onClick = navHost::removeLastOrNull
        )
    )
}



