/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.invites

import com.aiwazian.messenger.utils.UiText

sealed interface ChannelInviteLinkUiEffect {
    data class ShowSnackbar(val message: UiText) : ChannelInviteLinkUiEffect
}
