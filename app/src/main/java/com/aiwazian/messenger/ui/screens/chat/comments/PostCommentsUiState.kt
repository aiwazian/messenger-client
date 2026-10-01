/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.comments

import com.aiwazian.messenger.domain.Comment
import com.aiwazian.messenger.domain.MessageReplyPreview
import com.aiwazian.messenger.ui.screens.chat.ChatItem
import com.aiwazian.messenger.utils.DataStoreManager

data class PostCommentsUiState(
    val chatId: Long = -1,
    val postId: Long = -1,
    val myId: Long = -1,
    val isLoading: Boolean = true,
    val commentItems: List<ChatItem> = emptyList(),
    val commentCount: Int = 0,
    val commentText: String = "",
    val isSending: Boolean = false,
    val isSubscribed: Boolean = false,
    val isOwner: Boolean = false,
    val isAdmin: Boolean = false,
    val canDeleteComments: Boolean = false,
    val commentsEnabled: Boolean = false,
    val commentsRestrictedToSubscribers: Boolean = true,
    val keyboardHeight: Float = DataStoreManager.DEFAULT_KEYBOARD_HEIGHT,
    val replyToMessage: MessageReplyPreview? = null,
    val editingCommentId: Long? = null,
    val commentToDelete: Comment? = null
) {
    val canComment: Boolean
        get() = commentsEnabled &&
                (isOwner || isAdmin || !commentsRestrictedToSubscribers || isSubscribed)
}
