/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.privacy.blockedUsers

import com.aiwazian.messenger.domain.User

data class BlockedUserListUiState(
    val blockedUsers: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val showUnblockDialog: Boolean = false,
    val selectedUserToUnblock: User? = null
)
