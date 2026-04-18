/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings

sealed interface ChannelSettingsEffect {
    data object NavigateToMain : ChannelSettingsEffect
    data object NavigateToBack : ChannelSettingsEffect
    data class ShowSnackbar(val message: String) : ChannelSettingsEffect
}
