/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.transfer

import com.aiwazian.messenger.domain.User

data class ChannelTransferOwnershipUiState(
    val channelId: Long = 0,
    val subscribers: List<User> = emptyList(),
    val isLoading: Boolean = true,
    val isTransferring: Boolean = false,
    val selectedUser: User? = null
)
