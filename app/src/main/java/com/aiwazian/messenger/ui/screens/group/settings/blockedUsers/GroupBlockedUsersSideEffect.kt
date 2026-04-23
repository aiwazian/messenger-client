/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.blockedUsers

import com.aiwazian.messenger.utils.UiText

sealed interface GroupBlockedUsersSideEffect {
    data class ShowSnackbar(val message: UiText) : GroupBlockedUsersSideEffect
}
