/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.security

import android.content.ClipData
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
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
import com.aiwazian.messenger.ui.app.AppDialog
import com.aiwazian.messenger.ui.app.AppScaffold
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.utils.LottieAnimation
import kotlinx.coroutines.launch

@Composable
fun SettingsEmailConfigScreen(viewModel: EmailConfigViewModel = hiltViewModel()) {
    val navBackStack = LocalNavBackStack.current
    
    val email by viewModel.email.collectAsState()
    val showDisableDialog by viewModel.showDisableDialog.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                EmailConfigSideEffect.NavigateBack -> {
                    navBackStack.removeLastOrNull()
                }
                
                EmailConfigSideEffect.NavigateToChangeEmail -> {
                    navBackStack.add(AppRoute.SettingsEmail)
                }
            }
        }
    }
    
    if (showDisableDialog) {
        AppDialog(
            title = stringResource(R.string.remove_email),
            onDismissRequest = viewModel::hideDisableDialog,
            buttons = {
                TextButton(onClick = viewModel::hideDisableDialog) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(
                    onClick = viewModel::confirmDisableEmail,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.remove))
                }
            }
        ) {
            Text("Вы уверены, что хотите отключить электронный адрес?")
        }
    }
    
    AppScaffold(
        topBar = {
            PageTopBar()
        }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val composition by rememberLottieComposition(
                spec = LottieCompositionSpec.Asset(LottieAnimation.MAILBOX)
            )
            
            LottieAnimation(
                composition = composition,
                modifier = Modifier.size(100.dp),
                iterations = LottieConstants.IterateForever,
                isPlaying = true
            )
            
            Text(
                text = "Почта повышает безопасность аккаунта и помогает вернуть доступ, если вы забудете пароль.",
                fontSize = 14.sp,
                lineHeight = 14.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        SectionContainer {
            email?.let {
                SectionItem(headlineText = it, trailingContent = {
                    val clipboard = LocalClipboard.current
                    val scope = rememberCoroutineScope()
                    IconButton(
                        onClick = {
                            scope.launch {
                                clipboard.setClipEntry(
                                    ClipEntry(
                                        ClipData.newPlainText("user-email", email)
                                    )
                                )
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Rounded.ContentCopy, null)
                    }
                })
            }
            SectionItem(
                headlineText = stringResource(R.string.change_email),
                onClick = viewModel::onChangeEmailClick
            )
        }
        SectionContainer {
            SectionItem(
                headlineText = stringResource(R.string.remove_email),
                contentColor = MaterialTheme.colorScheme.error,
                onClick = viewModel::onDisableEmailClick
            )
        }
    }
}
