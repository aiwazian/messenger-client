/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.language

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import com.aiwazian.messenger.enums.AppLanguage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor() : ViewModel() {
    
    private val _currentLanguage = MutableStateFlow(
        AppLanguage.fromString(AppCompatDelegate.getApplicationLocales().toLanguageTags())
    )
    val currentLanguage = _currentLanguage.asStateFlow()
    
    fun selectLanguage(language: AppLanguage) {
        if (_currentLanguage.value == language) {
            return
        }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.code))
        _currentLanguage.update { language }
    }
}
