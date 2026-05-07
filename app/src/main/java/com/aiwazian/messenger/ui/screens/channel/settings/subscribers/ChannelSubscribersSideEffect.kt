/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.subscribers

import com.aiwazian.messenger.utils.UiText

sealed interface ChannelSubscribersSideEffect {
    data class ShowSnackbar(val message: UiText) : ChannelSubscribersSideEffect
}
