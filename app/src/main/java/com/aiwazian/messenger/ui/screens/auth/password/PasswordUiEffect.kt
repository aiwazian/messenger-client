/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.auth.password

import com.aiwazian.messenger.utils.UiText

sealed interface PasswordUiEffect {
    data class ShowSnackbar(val message: UiText) : PasswordUiEffect
    data object NavigateToMainActivity : PasswordUiEffect
    data class NavigateToPasswordResetCode(val login: String) : PasswordUiEffect
}
