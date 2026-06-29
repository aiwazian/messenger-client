/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.privacy.blockedUsers

sealed interface BlockedUserListSideEffect {
    data class ShowSnackbar(val message: String) : BlockedUserListSideEffect
}
