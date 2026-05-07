/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.auth.password

import com.aiwazian.messenger.utils.UiText

data class PasswordUiState(
    val login: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorText: UiText? = null
)
