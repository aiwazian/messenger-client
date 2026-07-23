/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.auth.password.reset

import com.aiwazian.messenger.ui.components.CodeInputStatus

data class PasswordResetCodeUiState(
    val login: String = "",
    val code: String = "",
    val codeStatus: CodeInputStatus = CodeInputStatus.Default,
    val isLoading: Boolean = false,
    val errorText: String? = null
)
