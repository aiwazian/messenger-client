/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.joinRequests

import com.aiwazian.messenger.utils.UiText

sealed interface ChannelJoinRequestsEffect {
    data class ShowSnackbar(val message: UiText) : ChannelJoinRequestsEffect
}
