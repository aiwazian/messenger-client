/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import com.aiwazian.messenger.enums.ChatType

/**
 * Какие категории чатов могут присылать уведомления.
 *
 * Всё включено по умолчанию: пока настройка не загружена, молчать хуже, чем показать
 * лишнее — пропущенное сообщение не вернёшь.
 */
data class NotificationSettings(
    val privateChats: Boolean = true,
    val groups: Boolean = true,
    val channels: Boolean = true
) {
    
    /** Неизвестный тип чата не подпадает ни под один переключатель — значит, не запрещён. */
    fun isEnabledFor(chatType: ChatType): Boolean = when (chatType) {
        ChatType.PRIVATE -> privateChats
        ChatType.GROUP -> groups
        ChatType.CHANNEL -> channels
        ChatType.UNKNOWN -> true
    }
}
