/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.comments

import com.aiwazian.messenger.utils.UiText

sealed interface ChannelCommentsSettingsEffect {
    data object NavigateToBack : ChannelCommentsSettingsEffect
    data class ShowSnackbar(val message: UiText) : ChannelCommentsSettingsEffect
}
