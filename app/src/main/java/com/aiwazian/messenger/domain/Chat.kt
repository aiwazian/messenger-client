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
    /** Бейдж непрочитанных в списке чатов. */
    val unreadCount: Int = 0,
    val firstUnreadMessageId: Long? = null,
    /** Помечен непрочитанным вручную: бейдж рисуется пустым. */
    val isManuallyUnread: Boolean = false,
    /** Уведомления по чату выключены: перечёркнутый колокольчик рядом с названием. */
    val isMuted: Boolean = false
) {
    /** Чат выглядит непрочитанным: есть сообщения либо стоит ручная пометка. */
    val isUnread: Boolean get() = unreadCount > 0 || isManuallyUnread
}
