/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

/**
 * Правила защиты контента чата.
 *
 * Логика вынесена из [ChatViewModel], `ChatScreen` и `MessageBubble` в отдельный класс,
 * чтобы все места интерфейса спрашивали один и тот же источник правды.
 *
 * Запрет распространяется на всех участников, включая владельца канала или группы.
 */
@JvmInline
value class ChatCopyPolicy(
    /** Включён ли запрет копирования в настройках канала или группы. */
    val noCopy: Boolean
) {
    
    /** Можно ли копировать текст сообщения в буфер обмена. */
    val canCopyText: Boolean
        get() = !noCopy
    
    /** Можно ли пересылать сообщения в другие чаты. */
    val canForward: Boolean
        get() = !noCopy
    
    /** Можно ли сохранять медиа в галерею или загрузки. */
    val canSaveMedia: Boolean
        get() = !noCopy
    
    /**
     * Можно ли делать скриншоты и записывать экран.
     *
     * Скриншот — такое же копирование содержимого, поэтому запрет закрывает и его.
     */
    val canTakeScreenshot: Boolean
        get() = !noCopy
    
    companion object {
        /** Обычный режим: ограничений нет. */
        val Unrestricted = ChatCopyPolicy(noCopy = false)
    }
}
