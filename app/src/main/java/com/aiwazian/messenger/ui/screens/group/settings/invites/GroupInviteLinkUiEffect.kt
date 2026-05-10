/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.invites

import com.aiwazian.messenger.utils.UiText

sealed interface GroupInviteLinkUiEffect {
    data class ShowSnackbar(val message: UiText) : GroupInviteLinkUiEffect
}
