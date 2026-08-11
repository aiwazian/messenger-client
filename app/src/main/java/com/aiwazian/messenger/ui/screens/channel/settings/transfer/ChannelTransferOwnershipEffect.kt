/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.transfer

import com.aiwazian.messenger.utils.UiText

sealed interface ChannelTransferOwnershipEffect {
    data object NavigateToMain : ChannelTransferOwnershipEffect
    data class ShowSnackbar(val message: UiText) : ChannelTransferOwnershipEffect
}
