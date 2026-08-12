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

/**
 * Кто и когда прочитал сообщение — в памяти, а не в Room.
 *
 * Сервер держит эти подробности трое суток и потом перестаёт их присылать. В Room
 * им поэтому не место: запись пережила бы срок хранения, и в меню сообщения навсегда
 * остался бы список читателей, которого на сервере уже нет. Room хранит сами
 * сообщения, а эфемерная часть ответа оседает здесь.
 *
 * Кэш общий на приложение: список просмотров не должен пропадать при выходе из чата
 * и повторном входе, пока данные не устарели.
 */
@Singleton
class MessageReadInfoCache @Inject constructor() {
    
    private val _readInfo = MutableStateFlow<Map<Long, List<MessageReadInfo>>>(emptyMap())
    val readInfo: StateFlow<Map<Long, List<MessageReadInfo>>> = _readInfo.asStateFlow()
    
    /**
     * Обновляет кэш по свежему ответу сервера.
     *
     * Пустой readInfo — это не «данных не было», а «читателей больше не показываем»:
     * так истёкший срок хранения убирает список просмотров, а не оставляет висеть
     * старый навсегда.
     */
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
    
    /** Сообщение удалено — читатели вместе с ним. */
    fun forget(messageId: Long) {
        _readInfo.update { current ->
            if (!current.containsKey(messageId)) current else current - messageId
        }
    }
    
    /**
     * Очистка истории.
     *
     * Чистится весь кэш, а не один чат: ключ здесь — id сообщения, и держать рядом
     * ещё и чат ради редкой операции незачем. Записи соседних чатов вернутся с первым
     * же их открытием.
     */
    fun clear() {
        _readInfo.value = emptyMap()
    }
}
