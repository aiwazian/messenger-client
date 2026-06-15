/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.profile

import com.aiwazian.messenger.domain.OwnedChannel

data class SelectChannelUiState(
    val channels: List<OwnedChannel> = emptyList(),
    val selectedChannelId: Long? = null,
    val isLoading: Boolean = false
)
