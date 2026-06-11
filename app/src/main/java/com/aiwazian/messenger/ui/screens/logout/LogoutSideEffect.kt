/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.logout


sealed interface LogoutSideEffect {
    object LogoutSuccess : LogoutSideEffect
    object SwitchToNextAccount : LogoutSideEffect
    object NoAccountsLeft : LogoutSideEffect
}
