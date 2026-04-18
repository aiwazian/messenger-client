/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.type

sealed interface ChannelTypeSettingsEffect {
    data object NavigateBack : ChannelTypeSettingsEffect
    data class ShowSnackbar(val message: String) : ChannelTypeSettingsEffect
}