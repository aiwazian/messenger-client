/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.invites.create

data class CreateGroupInviteLinkUiState(
    val maxUses: String = "",
    val expirationDate: Long? = null,
    val showDatePicker: Boolean = false,
    val requireApproval: Boolean = false
)
