/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.privacy

import androidx.annotation.StringRes

sealed class SettingsPrivacySideEffect {
    object ShowDeleteBottomSheet : SettingsPrivacySideEffect()
    object ShowDeleteDialog : SettingsPrivacySideEffect()
    data class ShowSnackbar(@param:StringRes val message: Int) : SettingsPrivacySideEffect()
    object NavigateToLogin : SettingsPrivacySideEffect()
}