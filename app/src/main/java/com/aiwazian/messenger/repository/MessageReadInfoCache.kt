/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.domain.MessageReadInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageReadInfoCache @Inject constructor() {
    
    private val _readInfo = MutableStateFlow<Map<Long, List<MessageReadInfo>>>(emptyMap())
    val readInfo: StateFlow<Map<Long, List<MessageReadInfo>>> = _readInfo.asStateFlow()

    fun update(messages: List<Message>) {
        if (messages.isEmpty()) return
        
        _readInfo.update { current ->
            val updated = current.toMutableMap()
            
            messages.forEach { message ->
                val info = message.readInfo
                
                if (info.isNullOrEmpty()) {
                    updated.remove(message.id)
                } else {
                    updated[message.id] = info
                }
            }
            
            updated
        }
    }

    fun addReader(messageIds: List<Long>, reader: MessageReadInfo) {
        if (messageIds.isEmpty()) return
        
        _readInfo.update { current ->
            val updated = current.toMutableMap()
            
            messageIds.forEach { messageId ->
                val existing = updated[messageId].orEmpty()
                
                updated[messageId] =
                    listOf(reader) + existing.filter { it.userId != reader.userId }
            }
            
            updated
        }
    }
    
    fun forget(messageId: Long) {
        _readInfo.update { current ->
            if (!current.containsKey(messageId)) current else current - messageId
        }
    }

    fun clear() {
        _readInfo.value = emptyMap()
    }
}
