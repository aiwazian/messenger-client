/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.members

import com.aiwazian.messenger.domain.User

data class GroupMembersState(
    val members: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
