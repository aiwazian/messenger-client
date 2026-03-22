/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.language

import androidx.lifecycle.ViewModel
import com.aiwazian.messenger.enums.AppLanguage
import com.aiwazian.messenger.utils.LanguageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val languageManager: LanguageManager
) : ViewModel() {

    val currentLanguage = languageManager.currentLanguage.asStateFlow()

    fun selectLanguage(language: AppLanguage) {
        if (currentLanguage.value == language) {
            return
        }
        languageManager.selLanguage(language)
    }
}



