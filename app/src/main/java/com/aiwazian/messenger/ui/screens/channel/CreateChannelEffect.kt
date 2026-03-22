/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel

import com.aiwazian.messenger.domain.Chat

sealed class CreateChannelEffect {
    data class NavigateToChat(val chat: Chat) : CreateChannelEffect()
}