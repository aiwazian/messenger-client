/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.management

import com.aiwazian.messenger.utils.UiText

sealed interface ChannelManagementEffect {
    data object NavigateToMain : ChannelManagementEffect
    data class ShowSnackbar(val message: UiText) : ChannelManagementEffect
}
