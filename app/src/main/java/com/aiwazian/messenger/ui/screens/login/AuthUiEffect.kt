/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.login

sealed class AuthUiEffect {
    data class ShowLoginDialog(val result: Boolean?) : AuthUiEffect()
    data object HideLoginDialog : AuthUiEffect()
    data object NavigateToPassword : AuthUiEffect()
    data class ShowPasswordDialog(
        val type: String,
        val errorMessage: String? = null
    ) : AuthUiEffect()
    
    data object HidePasswordDialog : AuthUiEffect()
    data object NavigateToMain : AuthUiEffect()
}