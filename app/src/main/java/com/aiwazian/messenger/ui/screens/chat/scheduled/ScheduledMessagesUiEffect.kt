/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.scheduled

import com.aiwazian.messenger.utils.UiText

sealed interface ScheduledMessagesUiEffect {
    data object NavigateBack : ScheduledMessagesUiEffect

    data class ShowSnackbar(val message: UiText) : ScheduledMessagesUiEffect

    data class OpenUrl(val url: String) : ScheduledMessagesUiEffect

    data class OpenEmail(val email: String) : ScheduledMessagesUiEffect

    data class OpenPhone(val phone: String) : ScheduledMessagesUiEffect

    data class NavigateToChat(
        val chatId: Long,
        val scrollToMessageId: Long? = null
    ) : ScheduledMessagesUiEffect
}
