/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

import com.aiwazian.messenger.domain.DropdownMenuAction
import com.aiwazian.messenger.domain.Message

sealed class ChatItem {
    data class DateSeparator(val text: String) : ChatItem()
    data class MessageItem(
        val message: Message,
        val time: String,
        val isMine: Boolean,
        val isRead: Boolean?,
        val senderName: String?,
        val isSingleEmoji: Boolean,
        val dropdownActions: List<DropdownMenuAction>
    ) : ChatItem()
}