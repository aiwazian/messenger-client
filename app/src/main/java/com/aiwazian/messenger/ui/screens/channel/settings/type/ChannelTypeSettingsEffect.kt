/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.type

sealed class ChannelTypeSettingsEffect {
    data object NavigateBack : ChannelTypeSettingsEffect()
    data class ShowError(val message: String) : ChannelTypeSettingsEffect()
}