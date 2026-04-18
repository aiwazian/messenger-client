/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.type

import com.aiwazian.messenger.enums.ChannelType

data class ChannelTypeSettingsUiState(
    val channelId: Long = 0,
    val publicLink: String? = null,
    val channelType: ChannelType = ChannelType.PRIVATE,
    val linkCheckStatus: LinkCheckStatus = LinkCheckStatus.Idle,
    val isLoading: Boolean = false,
    val canSave: Boolean = false,
)
