/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.auth.login

import com.aiwazian.messenger.utils.UiText

sealed interface LoginUiEffect {
    data class ShowSnackbar(val message: UiText) : LoginUiEffect
}
