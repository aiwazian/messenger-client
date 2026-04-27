/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.security

data class SettingsUiState(
    val deviceCount: Int = 0,
    val passcodeEnabled: Boolean = false,
    val showPasscodeBottomSheet: Boolean = false
)
