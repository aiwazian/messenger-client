/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Событие chat:unread — актуальный счётчик непрочитанных одного чата.
 *
 * Приходит и при новом сообщении (бейдж растёт на главном экране),
 * и после прочтения на другом устройстве (бейдж гаснет везде).
 */
@Serializable
data class ChatUnreadPayload(
    @SerialName("chatId") val chatId: Long,
    @SerialName("unreadCount") val unreadCount: Int = 0,
    @SerialName("firstUnreadMessageId") val firstUnreadMessageId: Long? = null,
    @SerialName("lastReadMessageId") val lastReadMessageId: Long? = null
)
