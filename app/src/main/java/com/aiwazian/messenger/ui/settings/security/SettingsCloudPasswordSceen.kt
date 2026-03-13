@file:OptIn(ExperimentalMaterial3Api::class)

package com.aiwazian.messenger.ui.settings.security

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aiwazian.messenger.R
import com.aiwazian.messenger.data.NavigationIcon
import com.aiwazian.messenger.ui.element.AnimatedIntroScreen
import com.aiwazian.messenger.ui.element.PageTopBar
import com.aiwazian.messenger.ui.element.SectionContainer
import com.aiwazian.messenger.ui.element.SectionItem
import com.aiwazian.messenger.utils.LottieAnimation
import com.aiwazian.messenger.viewModels.NavigationViewModel

@Composable
fun SettingsCloudPasswordScreen(enabled: Boolean = true) {
    Content(enabled)
}

@Composable
private fun Content(enabled: Boolean) {
    val navViewModel = viewModel<NavigationViewModel>()
    
    val navHost = rememberNavController()
    
    NavHost(
        navController = navHost,
        startDestination = if (enabled) "SETTINGS" else "MAIN"
    ) {
        composable("MAIN") {
            Main(navViewModel)
        }
        composable("SETTINGS") {
            Settings(navViewModel)
        }
    }
}

@Composable
private fun Main(navViewModel: NavigationViewModel) {
    Scaffold(topBar = {
        PageTopBar(
            navigationIcon = NavigationIcon(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                onClick = navViewModel::removeLastScreenInStack
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
                    navViewModel.addScreenInStack { }
                }
            )
        }
    }
}

@Composable
private fun Settings(navViewModel: NavigationViewModel) {
    Scaffold(
        topBar = {
            TopBar(navViewModel)
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            SectionContainer {
                SectionItem(
                    "Сменить пароль",
                    onClick = {
                        navViewModel.addScreenInStack {
                            SettingsChangeCloudPasswordScreen()
                        }
                    })
            }
        }
    }
}

@Composable
private fun TopBar(navViewModel: NavigationViewModel) {
    PageTopBar(
        title = {
            Text(stringResource(R.string.cloud_password))
        },
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            onClick = navViewModel::removeLastScreenInStack
        )
    )
}