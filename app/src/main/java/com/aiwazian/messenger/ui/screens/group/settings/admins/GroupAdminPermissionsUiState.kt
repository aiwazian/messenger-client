/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.admins

data class GroupAdminPermissionsUiState(
    val canManageInviteLinks: Boolean = false,
    val canEditProfile: Boolean = false,
    val canManageAdmins: Boolean = false,
    val canPinMessages: Boolean = false,
    val tag: String = "",
    val isSaving: Boolean = false,
    val isReadOnly: Boolean = false
)
