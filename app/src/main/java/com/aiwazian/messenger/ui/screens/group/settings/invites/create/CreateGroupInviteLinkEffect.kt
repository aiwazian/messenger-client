/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.invites.create

import com.aiwazian.messenger.utils.UiText

sealed interface CreateGroupInviteLinkEffect {
    data object Success : CreateGroupInviteLinkEffect
    data class ShowSnackbar(val text: UiText) : CreateGroupInviteLinkEffect
}
