/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.auth.password.reset

import com.aiwazian.messenger.utils.UiText

sealed interface ResetPasswordUiEffect {
    data class ShowSnackbar(val message: UiText) : ResetPasswordUiEffect
    data object NavigateToMainActivity : ResetPasswordUiEffect
}
