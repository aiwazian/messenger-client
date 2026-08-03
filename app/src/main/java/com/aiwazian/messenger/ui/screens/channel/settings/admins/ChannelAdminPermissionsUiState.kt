/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.admins

/** По умолчанию все разрешения выключены. */
data class ChannelAdminPermissionsUiState(
    val canManageInviteLinks: Boolean = false,
    val canEditProfile: Boolean = false,
    val isSaving: Boolean = false
)
