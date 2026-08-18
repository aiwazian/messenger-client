/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import com.aiwazian.messenger.domain.MessageReplyPreview
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Начатый ответ на сообщение — в памяти, а не в Room.
 *
 * ChatViewModel живёт ровно столько, сколько открыт экран чата, поэтому ответ
 * пропадал при выходе из чата: текст возвращался из черновика, а панель
 * «Ответить …» над полем ввода — нет. Кэш общий на приложение, и состояние
 * ответа переживает выход из чата и повторный вход.
 *
 * В Room ему не место: это незаконченное действие пользователя, а не данные
 * чата, и переживать перезапуск приложения оно не должно.
 *
 * Ключ — пара «аккаунт + чат»: на устройстве несколько аккаунтов, и ответ
 * одного из них не должен всплыть в том же чате у другого.
 */
@Singleton
class ReplyDraftCache @Inject constructor() {

    private val replies = ConcurrentHashMap<Key, MessageReplyPreview>()

    fun get(userId: Long, chatId: Long): MessageReplyPreview? = replies[Key(userId, chatId)]

    fun save(userId: Long, chatId: Long, preview: MessageReplyPreview) {
        replies[Key(userId, chatId)] = preview
    }

    fun clear(userId: Long, chatId: Long) {
        replies.remove(Key(userId, chatId))
    }

    private data class Key(val userId: Long, val chatId: Long)
}
