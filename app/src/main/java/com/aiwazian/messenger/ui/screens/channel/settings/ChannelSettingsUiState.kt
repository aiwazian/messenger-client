/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings

import com.aiwazian.messenger.domain.Channel
import com.aiwazian.messenger.enums.ChannelType

data class ChannelSettingsUiState(
    val channel: Channel = Channel(
        id = 0,
        name = "",
        bio = null,
        channelType = ChannelType.PRIVATE,
        username = null,
        subscribers = 0,
        removedUser = null,
        isSubscribed = false,
        ownerId = null
    ),
    val originalChannelData: Channel? = null,
    val hasChanges: Boolean = false,
    val showDeleteDialog: Boolean = false
)
