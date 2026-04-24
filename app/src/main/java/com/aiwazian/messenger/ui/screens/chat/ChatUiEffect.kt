/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

sealed class ChatUiEffect {
    data object NavigateBack : ChatUiEffect()
    data object NavigateToMain : ChatUiEffect()
    data class NavigateToChat(val chatId: Long) : ChatUiEffect()
    data class ShowSnackbar(val message: String) : ChatUiEffect()
    data class ScrollToBottom(val index: Int) : ChatUiEffect()
    data class OpenUrl(val url: String) : ChatUiEffect()
}
