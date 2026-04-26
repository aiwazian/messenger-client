/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.type

import com.aiwazian.messenger.utils.UiText

sealed interface ChannelTypeSettingsEffect {
    data object NavigateBack : ChannelTypeSettingsEffect
    data class ShowSnackbar(val message: UiText) : ChannelTypeSettingsEffect
}
