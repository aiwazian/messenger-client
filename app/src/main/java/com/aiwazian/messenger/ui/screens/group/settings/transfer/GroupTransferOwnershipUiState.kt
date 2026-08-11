/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.transfer

import com.aiwazian.messenger.domain.User

data class GroupTransferOwnershipUiState(
    val groupId: Long = 0,
    val members: List<User> = emptyList(),
    val isLoading: Boolean = true,
    val isTransferring: Boolean = false,
    val selectedUser: User? = null
)
