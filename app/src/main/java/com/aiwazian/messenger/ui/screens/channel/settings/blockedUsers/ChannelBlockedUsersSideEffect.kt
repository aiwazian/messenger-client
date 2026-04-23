/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.blockedUsers

import com.aiwazian.messenger.utils.UiText

sealed interface ChannelBlockedUsersSideEffect {
    data class ShowSnackbar(val message: UiText) : ChannelBlockedUsersSideEffect
}