/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.privacy.blockedUsers

import com.aiwazian.messenger.utils.UiText

sealed interface BlockedUserListSideEffect {
    data class ShowSnackbar(val message: UiText) : BlockedUserListSideEffect
}
