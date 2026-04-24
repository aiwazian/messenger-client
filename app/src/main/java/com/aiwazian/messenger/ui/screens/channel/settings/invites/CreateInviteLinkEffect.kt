/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.invites

sealed interface CreateInviteLinkEffect {
    data object Success : CreateInviteLinkEffect
}
