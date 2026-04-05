/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.domain.Message

sealed class ChatUiEffect {
    data object NavigateBack : ChatUiEffect()
    data object NavigateToMain : ChatUiEffect()
    data class ScrollToBottom(val index: Int) : ChatUiEffect()
    data class NotifyMainMessageSent(val message: Message) : ChatUiEffect()
    data class NotifyMainChatDeleted(val chatId: Long) : ChatUiEffect()
    data class NotifyMainNewChat(
        val chat: Chat,
        val lastMessage: Message?
    ) : ChatUiEffect()
    data class ShowInviteSnackbar(val message: String) : ChatUiEffect()
    data class NavigateToChat(val chatId: Long) : ChatUiEffect()
}