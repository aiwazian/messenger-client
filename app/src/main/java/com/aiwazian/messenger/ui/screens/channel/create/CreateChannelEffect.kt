/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.create

import com.aiwazian.messenger.utils.UiText

sealed interface CreateChannelEffect {
    data class ShowSnackbar(val message: UiText) : CreateChannelEffect
    data class NavigateToChat(val chatId: Long) : CreateChannelEffect
}
