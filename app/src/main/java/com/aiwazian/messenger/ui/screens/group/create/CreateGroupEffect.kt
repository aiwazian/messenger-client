/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.create

import com.aiwazian.messenger.utils.UiText

sealed interface CreateGroupEffect {
    data class ShowSnackbar(val message: UiText) : CreateGroupEffect
    data class NavigateToChat(val chatId: Long) : CreateGroupEffect
}