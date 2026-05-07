/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.auth.register

import com.aiwazian.messenger.utils.UiText

sealed interface RegisterUiEffect {
    data object NavigateToMainActivity : RegisterUiEffect
    data class ShowSnackbar(val message: UiText) : RegisterUiEffect
}
