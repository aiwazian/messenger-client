/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.language

import com.aiwazian.messenger.enums.AppLanguage

data class LanguageUiState(
    val downloadingLanguage: AppLanguage? = null,
    val showStopDialog: Boolean = false,
    val requiresConfirmation: Boolean = false
)
