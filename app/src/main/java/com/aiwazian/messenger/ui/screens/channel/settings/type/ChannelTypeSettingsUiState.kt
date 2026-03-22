/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.type

import com.aiwazian.messenger.domain.Channel
import com.aiwazian.messenger.enums.ChannelType

data class ChannelTypeSettingsUiState(
    val channel: Channel = Channel(),
    val channelType: ChannelType = ChannelType.PRIVATE,
    val publicLink: String = "",
    val inviteLink: String? = null,
    val linkCheckStatus: LinkCheckStatus = LinkCheckStatus.Idle,
    val isLoading: Boolean = false,
    val canSave: Boolean = false,
    val error: String? = null
)
