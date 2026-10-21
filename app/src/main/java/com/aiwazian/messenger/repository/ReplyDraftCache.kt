/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import com.aiwazian.messenger.domain.MessageReplyPreview
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

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
