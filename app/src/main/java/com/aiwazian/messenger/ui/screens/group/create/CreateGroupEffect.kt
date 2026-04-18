/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.create

sealed interface CreateGroupEffect {
    data class ShowSnackbar(val message: String) : CreateGroupEffect
    data class NavigateToChat(val chatId: Long) : CreateGroupEffect
}