/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.create

import com.aiwazian.messenger.domain.Chat

sealed class CreateGroupEffect {
    data class NavigateToChat(val chat: Chat) : CreateGroupEffect()
}