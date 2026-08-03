/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.admins

/** По умолчанию все разрешения выключены. */
data class GroupAdminPermissionsUiState(
    val canManageInviteLinks: Boolean = false,
    val canEditProfile: Boolean = false,
    val tag: String = "",
    val isSaving: Boolean = false
)
