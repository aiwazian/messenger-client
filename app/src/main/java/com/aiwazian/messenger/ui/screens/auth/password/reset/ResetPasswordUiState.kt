/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.auth.password.reset

import com.aiwazian.messenger.utils.UiText

data class ResetPasswordUiState(
    val login: String = "",
    val code: String = "",
    val newPassword: String = "",
    val isLoading: Boolean = false,
    val errorText: UiText? = null
)
