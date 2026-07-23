/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.security

sealed interface EmailConfigSideEffect {
    data object NavigateBack : EmailConfigSideEffect
    data object NavigateToChangeEmail : EmailConfigSideEffect
}
