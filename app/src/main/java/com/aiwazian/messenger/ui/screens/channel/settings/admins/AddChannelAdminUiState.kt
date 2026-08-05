/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.admins

import com.aiwazian.messenger.domain.User

data class AddChannelAdminUiState(
    val subscribers: List<User> = emptyList()
)
