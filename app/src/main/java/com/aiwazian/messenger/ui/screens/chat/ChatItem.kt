/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.domain.MessageReadInfo
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.ui.components.topBar.DropdownMenuAction
import com.aiwazian.messenger.utils.UiText

sealed interface ChatItem {
    data class DateSeparator(val text: String) : ChatItem
    data object UnreadSeparator : ChatItem
    data class SystemMessage(val text: UiText, val sendTime: Long) : ChatItem
    data class MessageItem(
        val message: Message,
        val time: String,
        val isMine: Boolean,
        val isRead: Boolean?,
        val senderName: String?,
        val isFirstInGroup: Boolean,
        val isSingleEmoji: Boolean,
        val dropdownActions: List<DropdownMenuAction>,
        val chatType: ChatType,
        val isHighlighted: Boolean = false,
        val readInfo: List<MessageReadInfo>? = null,
        /**
         * Доступен ли ответ на это сообщение.
         *
         * Тем же флагом включается свайп влево по сообщению: свайп — это просто
         * второй способ вызвать «Ответить».
         */
        val canReply: Boolean = false,
        /**
         * Тег отправителя: подпись рядом с его именем.
         *
         * Есть только в группах и только у тех, кому владелец его выдал.
         */
        val senderTag: String? = null
    ) : ChatItem
}
