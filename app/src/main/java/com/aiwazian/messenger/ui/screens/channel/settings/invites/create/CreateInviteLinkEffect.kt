/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.invites.create

import com.aiwazian.messenger.utils.UiText

sealed interface CreateInviteLinkEffect {
    data object Success : CreateInviteLinkEffect
    data class ShowSnackbar(val message: UiText) : CreateInviteLinkEffect
}
