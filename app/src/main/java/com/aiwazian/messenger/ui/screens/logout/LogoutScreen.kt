/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.logout

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.AuthActivity
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.components.CustomDialog
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionHeader
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LogoutScreen(viewModel: LogoutViewModel = hiltViewModel()) {
    val navBackStack = LocalNavBackStack.current
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collectLatest { sideEffect ->
            when (sideEffect) {
                is LogoutSideEffect.LogoutSuccess -> {
                    val intent = Intent(
                        context,
                        AuthActivity::class.java
                    ).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    context.startActivity(intent)
                    (context as? Activity)?.finish()
                }
            }
        }
    }
    
    Scaffold(
        topBar = { TopBar() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .verticalScroll(scrollState)
        ) {
            SectionContainer(header = {
                SectionHeader(title = stringResource(R.string.alternative_options))
            }) {
                SectionItem(
                    leadingIcon = Icons.Rounded.DeleteOutline,
                    headlineText = stringResource(R.string.clear_cache),
                    supportingText = "Освободите память устройства, файлы останутся в облаке.",
                    onClick = {
                        navBackStack.add(AppRoute.SettingsDataAndStorage)
                    })
            }
            
            SectionContainer {
                SectionItem(
                    headlineText = stringResource(R.string.logout),
                    contentColor = MaterialTheme.colorScheme.error,
                    onClick = viewModel::showLogoutDialog
                )
            }
            
            if (uiState.isLogoutDialogVisible) {
                LogoutModal(
                    onConfirm = viewModel::logout,
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
        title = stringResource(R.string.logout),
        onDismissRequest = onDismiss,
        content = {
            Text(text = stringResource(R.string.logout_confirm_message))
        },
        buttons = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.logout))
            }
        })
}

@Composable
private fun TopBar() {
    val navBackStack = LocalNavBackStack.current
    
    PageTopBar(
        title = { Text(stringResource(R.string.logout)) },
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            onClick = navBackStack::removeLastOrNull
        )
    )
}
