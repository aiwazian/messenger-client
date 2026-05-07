/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.auth.register

import com.aiwazian.messenger.utils.UiText

data class RegisterUiState(
    val isLoading: Boolean = false,
    val login: String = "",
    val password: String = "",
    val firstName: String = "",
    val passwordFieldError: UiText? = null,
    val firstNameFieldError: UiText? = null
)
