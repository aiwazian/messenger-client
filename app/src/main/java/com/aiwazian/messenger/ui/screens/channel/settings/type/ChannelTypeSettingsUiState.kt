/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.type

import com.aiwazian.messenger.enums.ChannelType

data class ChannelTypeSettingsUiState(
    val channelId: Long = -1,
    val username: String = "",
    val channelType: ChannelType = ChannelType.PRIVATE,
    val linkCheckStatus: LinkCheckStatus = LinkCheckStatus.Idle,
    val canSave: Boolean = false,
)
