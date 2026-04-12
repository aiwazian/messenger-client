/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.blockedUsers

import com.aiwazian.messenger.domain.User

data class GroupBlockedUsersState(
    val blockedUsers: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface GroupBlockedUsersSideEffect {
    data class ShowSnackbar(val message: String) : GroupBlockedUsersSideEffect
    data object ShowUnblockConfirmation : GroupBlockedUsersSideEffect
}
