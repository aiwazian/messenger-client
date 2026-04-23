/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.blockedUsers

import com.aiwazian.messenger.domain.User

data class ChannelBlockedUsersState(
    val channelId: Long = -1,
    val blockedUsers: List<User> = emptyList(),
    val showUnblockDialog: Boolean = false
)
