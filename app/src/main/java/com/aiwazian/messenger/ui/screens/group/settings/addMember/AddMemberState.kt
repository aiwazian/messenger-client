/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.addMember

import com.aiwazian.messenger.domain.User

data class AddMemberState(
    val users: List<User> = emptyList(),
    val selectedUserIds: Set<Long> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null
)
