/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

import com.aiwazian.messenger.utils.UiText

sealed interface ChatUiEffect {
    data object NavigateBack : ChatUiEffect
    data object NavigateToMain : ChatUiEffect
    data class NavigateToChat(val chatId: Long) : ChatUiEffect
    data class ShowSnackbar(val message: UiText) : ChatUiEffect
    data class ScrollToBottom(val index: Int) : ChatUiEffect
    data class OpenUrl(val url: String) : ChatUiEffect
}
