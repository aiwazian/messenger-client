/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.logout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.CustomDialog
import com.aiwazian.messenger.ui.components.navigation.LocalNavHost
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionHeader
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import kotlinx.coroutines.launch

@Composable
fun LogoutScreen(viewModel: LogoutViewModel = hiltViewModel()) {
    val navHost = LocalNavHost.current
    val scrollState = rememberScrollState()
    
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = { TopBar() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .verticalScroll(scrollState)
        ) {
            SectionHeader(title = stringResource(R.string.alternative_options))

            SectionContainer {
                SectionItem(
                    icon = Icons.Rounded.Delete,
                    text = stringResource(R.string.clear_cache),
                    description = "Освободите память устройства, файлы останутся в облаке.",
                    onClick = {
                        navHost.add(AppRoute.SettingsDataAndStorage)
                    })
            }

            SectionContainer {
                SectionItem(
                    text = stringResource(R.string.log_out),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    onClick = viewModel::showLogoutDialog
                )
            }

            if (uiState.isLogoutDialogVisible) {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()

                LogoutModal(
                    onConfirm = {
                        scope.launch {
                            viewModel.logout(context)
                        }
                    },
                    onDismiss = viewModel::hideLogoutDialog
                )
            }
        }
    }
}

@Composable
private fun LogoutModal(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    CustomDialog(
        title = stringResource(R.string.log_out),
        onDismissRequest = onDismiss,
        content = {
            Text(text = "Вы точно хотите выйти?")
        },
        buttons = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.log_out))
            }
        })
}

@Composable
private fun TopBar() {
    val navHost = LocalNavHost.current

    PageTopBar(
        title = { Text(stringResource(R.string.log_out)) },
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            onClick = navHost::removeLastOrNull
        )
    )
}
