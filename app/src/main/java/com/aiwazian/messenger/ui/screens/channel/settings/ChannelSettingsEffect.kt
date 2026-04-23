/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings

import com.aiwazian.messenger.utils.UiText

sealed interface ChannelSettingsEffect {
    data object NavigateToMain : ChannelSettingsEffect
    data object NavigateToBack : ChannelSettingsEffect
    data class ShowSnackbar(val message: UiText) : ChannelSettingsEffect
}
