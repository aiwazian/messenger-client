/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.auth.password.reset

import com.aiwazian.messenger.utils.UiText

sealed interface PasswordResetCodeUiEffect {
    data class ShowSnackbar(val message: UiText) : PasswordResetCodeUiEffect
    data class NavigateToResetPassword(
        val login: String,
        val code: String
    ) : PasswordResetCodeUiEffect
}
