/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.subscribers

import com.aiwazian.messenger.domain.User

data class ChannelSubscribersUiState(
    val subscribers: List<User> = emptyList(),
    val showKickDialog: Boolean = false,
    val showBlockDialog: Boolean = false,
    val searchQuery: String = "",
    val selectedUserId: Long? = null
)
