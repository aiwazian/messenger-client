/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.joinRequests

import com.aiwazian.messenger.domain.User

data class ChannelJoinRequestsState(
    val requests: List<User> = emptyList(),
    val isLoading: Boolean = true
)
