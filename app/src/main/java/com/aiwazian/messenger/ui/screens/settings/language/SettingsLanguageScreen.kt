/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.language

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.AppLanguage
import com.aiwazian.messenger.ui.app.AppDialog
import com.aiwazian.messenger.ui.app.AppScaffold
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionRadio
import com.aiwazian.messenger.ui.components.topBar.PageTopBar

@Composable
fun SettingsLanguageScreen(languageViewModel: LanguageViewModel = hiltViewModel()) {
    val currentLanguage by languageViewModel.currentLanguage.collectAsState()
    val uiState by languageViewModel.uiState.collectAsState()

    val confirmationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { languageViewModel.onConfirmationResult() }

    LaunchedEffect(uiState.requiresConfirmation) {
        if (uiState.requiresConfirmation) {
            languageViewModel.launchUserConfirmation(confirmationLauncher)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppScaffold(
            topBar = {
                PageTopBar(
                    title = { Text(stringResource(R.string.language)) },
                )
            }
        ) {
            SectionContainer {
                AppLanguage.entries.sortedBy { it.displayName.lowercase() }.forEach { language ->
                    SectionRadio(
                        headlineText = language.nativeName,
                        selected = currentLanguage == language,
                        supportingText = language.displayName,
                        onClick = {
                            languageViewModel.selectLanguage(language)
                        })
                }
            }
        }

        if (uiState.downloadingLanguage != null) {
            LanguageDownloadOverlay(
                onBackgroundTap = languageViewModel::showStopDialog
            )
        }

        if (uiState.showStopDialog) {
            LanguageDownloadStopDialog(
                onConfirm = languageViewModel::confirmStopDownload,
                onDismiss = languageViewModel::hideStopDialog
            )
        }
    }
}

@Composable
private fun LanguageDownloadOverlay(onBackgroundTap: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onBackgroundTap
            ),
        contentAlignment = Alignment.Center
    ) {
        CircularWavyProgressIndicator()
    }
}

@Composable
private fun LanguageDownloadStopDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AppDialog(
        title = stringResource(R.string.language_download_stop_title),
        onDismissRequest = onDismiss,
        content = {
            Text(text = stringResource(R.string.language_download_stop_message))
        },
        buttons = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.no))
            }
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.yes))
            }
        }
    )
}
