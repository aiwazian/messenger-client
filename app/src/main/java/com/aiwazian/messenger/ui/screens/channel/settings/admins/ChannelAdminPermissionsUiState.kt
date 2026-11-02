/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.admins

data class ChannelAdminPermissionsUiState(
    val canManageInviteLinks: Boolean = false,
    val canEditProfile: Boolean = false,
    val canManageAdmins: Boolean = false,
    val canPinMessages: Boolean = false,
    val canDeleteComments: Boolean = false,
    val isSaving: Boolean = false,
    val isReadOnly: Boolean = false
)
