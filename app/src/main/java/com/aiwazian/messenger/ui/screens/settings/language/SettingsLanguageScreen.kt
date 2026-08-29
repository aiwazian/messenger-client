/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.language

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.AppLanguage
import com.aiwazian.messenger.ui.app.AppScaffold
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionRadioItem
import com.aiwazian.messenger.ui.components.topBar.PageTopBar

@Composable
fun SettingsLanguageScreen(languageViewModel: LanguageViewModel = hiltViewModel()) {
    val currentLanguage by languageViewModel.currentLanguage.collectAsState()
    
    AppScaffold(
        topBar = {
            PageTopBar(
                title = { Text(stringResource(R.string.language)) },
            )
        }
    ) {
        SectionContainer {
            AppLanguage.entries.forEach { language ->
                SectionRadioItem(
                    text = language.nativeName,
                    selected = currentLanguage == language,
                    description = language.displayName,
                    onClick = {
                        languageViewModel.selectLanguage(language)
                    })
            }
        }
    }
}
