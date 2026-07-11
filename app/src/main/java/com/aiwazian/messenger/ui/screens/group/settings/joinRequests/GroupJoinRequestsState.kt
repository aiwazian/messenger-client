/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.joinRequests

import com.aiwazian.messenger.domain.User

data class GroupJoinRequestsState(
    val requests: List<User> = emptyList(),
    val isLoading: Boolean = true
)
