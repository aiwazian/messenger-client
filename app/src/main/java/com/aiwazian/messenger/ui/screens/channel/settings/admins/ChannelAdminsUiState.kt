/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.admins

import com.aiwazian.messenger.domain.ChannelAdmin

data class ChannelAdminsUiState(
    val admins: List<ChannelAdmin> = emptyList(),
    val isLoading: Boolean = false,
    val selectedUserId: Long? = null,
    val showDemoteDialog: Boolean = false,
    /** Нужен, чтобы в своей карточке не показывать действия над собой. */
    val currentUserId: Long? = null
)
