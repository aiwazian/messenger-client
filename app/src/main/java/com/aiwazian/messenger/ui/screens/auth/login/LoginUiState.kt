/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.auth.login

import com.aiwazian.messenger.utils.UiText

data class LoginUiState(
    val login: String = "",
    val isLoading: Boolean = false,
    val showFoundDialog: Boolean = false,
    val showNotFoundDialog: Boolean = false,
    val canReset: Boolean = false,
    val errorText: UiText? = null
)
