/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.profile

import androidx.compose.ui.graphics.Color

sealed interface UsernameUiState {
    data object Idle : UsernameUiState
    data object Checking : UsernameUiState
    data object Available : UsernameUiState
    data class Unavailable(val message: String) : UsernameUiState
    data class Invalid(val message: String) : UsernameUiState
}

data class UsernameScreenUiState(
    val username: String = "",
    val status: UsernameUiState = UsernameUiState.Idle
) {
    val isAvailable: Boolean get() = status is UsernameUiState.Available
    val isChecking: Boolean get() = status is UsernameUiState.Checking
    val isError: Boolean get() = status is UsernameUiState.Unavailable || status is UsernameUiState.Invalid
    val messageText: String? get() = when(status) {
        is UsernameUiState.Checking -> "Проверка"
        is UsernameUiState.Available -> "Имя пользователя доступно"
        is UsernameUiState.Unavailable -> "Имя пользователя занято"
        is UsernameUiState.Invalid -> status.message
        is UsernameUiState.Idle -> null
    }
    val messageColor: Color get() = when(status) {
        is UsernameUiState.Available -> Color.Green
        is UsernameUiState.Unavailable, is UsernameUiState.Invalid -> Color.Red
        else -> Color.Gray
    }
}
