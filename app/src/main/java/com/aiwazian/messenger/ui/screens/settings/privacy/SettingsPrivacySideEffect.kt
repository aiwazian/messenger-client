/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.privacy

import com.aiwazian.messenger.utils.UiText

sealed interface SettingsPrivacySideEffect {
    object NavigateToLogin : SettingsPrivacySideEffect
    data class ShowSnackbar(val message: UiText) : SettingsPrivacySideEffect
}
