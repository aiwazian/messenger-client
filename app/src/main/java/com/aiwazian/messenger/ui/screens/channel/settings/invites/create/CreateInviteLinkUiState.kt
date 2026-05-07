/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.invites.create


data class CreateInviteLinkUiState(
    val maxUses: String = "",
    val expirationDate: Long? = null,
    val showDatePicker: Boolean = false
)
