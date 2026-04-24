/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.security.passcode

sealed interface PasscodeUiEffect {
    data object NavigateBack : PasscodeUiEffect
}
