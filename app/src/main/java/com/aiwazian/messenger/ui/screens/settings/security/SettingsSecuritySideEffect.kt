/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.security

import com.aiwazian.messenger.utils.UiText

sealed interface SettingsSecuritySideEffect {
    data class ShowSnackbar(val message: UiText) : SettingsSecuritySideEffect
    data object NavigateToCloudPassword : SettingsSecuritySideEffect
    data object NavigateToLogin : SettingsSecuritySideEffect
}
