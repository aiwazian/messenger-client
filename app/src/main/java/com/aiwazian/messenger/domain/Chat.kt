/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import android.net.Uri
import com.aiwazian.messenger.utils.UiText

data class Chat(
    val id: Long,
    val chatName: UiText,
    val isPinned: Boolean,
    val avatarUri: Uri? = null,
    val lastMessage: Message?,
    val draftText: String? = null,
    val unreadCount: Int = 0,
    val firstUnreadMessageId: Long? = null,
    val isManuallyUnread: Boolean = false,
    val isMuted: Boolean = false
) {
    val isUnread: Boolean get() = unreadCount > 0 || isManuallyUnread
}
