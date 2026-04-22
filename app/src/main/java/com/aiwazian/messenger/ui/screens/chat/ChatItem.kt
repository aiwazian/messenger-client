/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.ui.components.topBar.DropdownMenuAction
import com.aiwazian.messenger.utils.UiText

sealed class ChatItem {
    data class DateSeparator(val text: String) : ChatItem()
    data class SystemMessage(val text: UiText, val sendTime: Long) : ChatItem()
    data class MessageItem(
        val message: Message,
        val time: String,
        val isMine: Boolean,
        val isRead: Boolean?,
        val senderName: String?,
        val isFirstInGroup: Boolean,
        val isSingleEmoji: Boolean,
        val dropdownActions: List<DropdownMenuAction>
    ) : ChatItem()
}