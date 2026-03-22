/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.aiwazian.messenger.enums.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanguageManager @Inject constructor() {
    val currentLanguage =
        MutableStateFlow(
            AppLanguage.fromString(
                AppCompatDelegate.getApplicationLocales()
                    .toLanguageTags()
            )
        )
    
    fun selLanguage(language: AppLanguage) {
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(language.code)
        AppCompatDelegate.setApplicationLocales(appLocale)
        currentLanguage.update { language }
    }
}
