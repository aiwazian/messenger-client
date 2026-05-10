/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.invites.create

import com.aiwazian.messenger.utils.UiText

sealed interface CreateChannelInviteLinkEffect {
    data object NavigateBack : CreateChannelInviteLinkEffect
    data class ShowSnackbar(val message: UiText) : CreateChannelInviteLinkEffect
}
