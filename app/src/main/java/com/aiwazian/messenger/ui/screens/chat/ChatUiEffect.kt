/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

import com.aiwazian.messenger.utils.UiText

sealed interface ChatUiEffect {
    data object NavigateBack : ChatUiEffect
    data object NavigateToMain : ChatUiEffect
    
    /**
     * Переход в другой чат.
     *
     * scrollToMessageId — если надо сразу прыгнуть к конкретному сообщению
     * (клик по ответу на сообщение из другого чата).
     */
    data class NavigateToChat(
        val chatId: Long,
        val scrollToMessageId: Long? = null
    ) : ChatUiEffect
    data class ShowSnackbar(val message: UiText) : ChatUiEffect
    data class ScrollToBottom(val index: Int) : ChatUiEffect
    data class OpenUrl(val url: String) : ChatUiEffect
    
    /** Открыть почтовый адрес в почтовом клиенте устройства. */
    data class OpenEmail(val email: String) : ChatUiEffect
}
