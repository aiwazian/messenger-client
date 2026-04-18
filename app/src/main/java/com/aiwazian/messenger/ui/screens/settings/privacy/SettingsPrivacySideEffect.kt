/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.privacy

import androidx.annotation.StringRes

sealed interface SettingsPrivacySideEffect {
    object NavigateToLogin : SettingsPrivacySideEffect
    data class ShowSnackbar(@param:StringRes val message: Int) : SettingsPrivacySideEffect
}
