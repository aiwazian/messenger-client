/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.comments

import com.aiwazian.messenger.utils.UiText

sealed interface PostCommentsUiEffect {
    data class ShowSnackbar(val message: UiText) : PostCommentsUiEffect
    data class ShowMediaViewer(val fileId: String) : PostCommentsUiEffect
    data class ScrollToComment(val commentId: Long) : PostCommentsUiEffect
}
