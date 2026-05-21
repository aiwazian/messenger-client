/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object ActiveChatTracker {
    private val chatStack = mutableListOf<Long>()
    private val _activeChatId = MutableStateFlow<Long?>(null)
    val activeChatId = _activeChatId.asStateFlow()
    
    @Synchronized
    fun pushChat(chatId: Long) {
        chatStack.remove(chatId)
        chatStack.add(chatId)
        _activeChatId.value = chatId
    }
    
    @Synchronized
    fun popChat(chatId: Long) {
        chatStack.remove(chatId)
        _activeChatId.value = chatStack.lastOrNull()
    }
}
