/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.create

sealed interface CreateChannelEffect {
    data class ShowSnackbar(val message: String) : CreateChannelEffect
    data class NavigateToChat(val chatId: Long) : CreateChannelEffect
}
