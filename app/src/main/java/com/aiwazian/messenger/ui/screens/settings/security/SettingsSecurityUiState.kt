/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.security

data class SettingsSecurityUiState(
    val deviceCount: Int = 1,
    val passcodeEnabled: Boolean = false,
    val showPasscodeBottomSheet: Boolean = false,
    val showEmailBottomSheet: Boolean = false,
    val email: String? = null
)
