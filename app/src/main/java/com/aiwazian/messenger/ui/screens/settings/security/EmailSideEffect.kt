/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.security

sealed interface EmailSideEffect {
    data object NavigateToVerify : EmailSideEffect
}
