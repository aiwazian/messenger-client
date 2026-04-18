/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.subscribers

sealed interface ChannelSubscribersSideEffect {
    data class ShowSnackbar(val message: String) : ChannelSubscribersSideEffect
}
