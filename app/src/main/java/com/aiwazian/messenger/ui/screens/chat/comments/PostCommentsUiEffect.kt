/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.comments

import com.aiwazian.messenger.utils.UiText

sealed interface PostCommentsUiEffect {
    data class ShowSnackbar(val message: UiText) : PostCommentsUiEffect
    data class ShowMediaViewer(val fileId: String) : PostCommentsUiEffect
    data class ScrollToComment(val commentId: Long) : PostCommentsUiEffect
    data class OpenUrl(val url: String) : PostCommentsUiEffect
    data class OpenEmail(val email: String) : PostCommentsUiEffect
    data class OpenPhone(val phone: String) : PostCommentsUiEffect
    data class NavigateToChat(val chatId: Long) : PostCommentsUiEffect
}
