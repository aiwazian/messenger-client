package com.aiwazian.messenger.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Key
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.data.NavigationIcon
import com.aiwazian.messenger.services.AppLockService
import com.aiwazian.messenger.ui.element.CustomDialog
import com.aiwazian.messenger.ui.element.PageTopBar
import com.aiwazian.messenger.ui.element.SectionContainer
import com.aiwazian.messenger.ui.element.SectionHeader
import com.aiwazian.messenger.ui.element.SectionItem
import com.aiwazian.messenger.ui.settings.SettingsDataAndStorageScreen
import com.aiwazian.messenger.ui.settings.security.SettingsPasscodeScreen
import com.aiwazian.messenger.viewModels.LogoutViewModel
import com.aiwazian.messenger.viewModels.NavigationViewModel
import kotlinx.coroutines.launch

@Composable
fun LogoutScreen() {
    Content()
}

@Composable
private fun Content() {
    val navViewModel = viewModel<NavigationViewModel>()
    val logoutViewModel = hiltViewModel<LogoutViewModel>()
    
    val scrollState = rememberScrollState()
    
    val logoutDialog = logoutViewModel.logoutDialog
    
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
                //                SectionItem(
                //                    icon = Icons.Outlined.PersonAdd,
                //                    text = "Добавить аккаунт",
                //                    description = "Подключите несколько аккаунтов и легко переключайтесь между ними."
                //                )
                //
                SectionItem(
                    icon = Icons.Outlined.Delete,
                    text = stringResource(R.string.clear_cache),
                    description = "Освободите память устройства, файлы останутся в облаке.",
                    onClick = {
                        navViewModel.addScreenInStack {
                            SettingsDataAndStorageScreen()
                        }
                    })
                
                val hasPasscode by AppLockService().hasPasscode.collectAsState()
                
                if (!hasPasscode) {
                    SectionItem(
                        icon = Icons.Outlined.Key,
                        text = "Установить код пароль",
                        description = "Включите код-пароль для разблокировки приложения на Вашем устройстве.",
                        onClick = {
                            navViewModel.addScreenInStack {
                                SettingsPasscodeScreen()
                            }
                        })
                }
            }
            
            SectionContainer {
                SectionItem(
                    text = stringResource(R.string.log_out),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    onClick = logoutDialog::show
                )
            }
            
            if (logoutDialog.isVisible) {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                
                LogoutModal(
                    onConfirm = {
                        scope.launch {
                            logoutViewModel.logout(context)
                        }
                    },
                    onDismiss = logoutDialog::hide
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
    val navViewModel = viewModel<NavigationViewModel>()
    
    PageTopBar(
        title = { Text(stringResource(R.string.log_out)) },
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            onClick = navViewModel::removeLastScreenInStack
        )
    )
}