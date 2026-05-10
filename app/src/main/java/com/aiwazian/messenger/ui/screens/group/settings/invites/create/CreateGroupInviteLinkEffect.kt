/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.invites.create

import com.aiwazian.messenger.utils.UiText

sealed interface CreateGroupInviteLinkEffect {
    data object NavigateBack : CreateGroupInviteLinkEffect
    data class ShowSnackbar(val message: UiText) : CreateGroupInviteLinkEffect
}
