/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.privacy

sealed class SettingsPrivacySideEffect {
    object ShowDeleteBottomSheet : SettingsPrivacySideEffect()
    object ShowDeleteDialog : SettingsPrivacySideEffect()
    data class ShowSnackbar(val message: String) : SettingsPrivacySideEffect()
    object NavigateToLogin : SettingsPrivacySideEffect()
}