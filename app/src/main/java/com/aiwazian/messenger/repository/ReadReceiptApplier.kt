/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import com.aiwazian.messenger.database.dao.MessageDao
import com.aiwazian.messenger.domain.MessageReadInfo
import com.aiwazian.messenger.domain.ReadMessagePayload
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Событие прочтения из сокета -> время прочтения в меню сообщения.
 *
 * Галочки ставились сразу, а «Прочитано в 17:10» появлялось только после перезахода
 * в чат: флаг isRead живёт в Room и обновлялся по событию, а подробности приезжали
 * только с ответом на загрузку истории. Здесь событие достраивается до той же формы,
 * что и ответ сервера.
 *
 * Отдельный класс, а не метод в ChatRepository: единственный вызывающий — слушатель
 * сокета, а зависимости у логики свои.
 */
@Singleton
class ReadReceiptApplier @Inject constructor(
    private val messageDao: MessageDao,
    private val readInfoCache: MessageReadInfoCache,
    private val userRepository: UserRepository
) {
    
    /**
     * Собеседник прочитал чат до определённого момента.
     *
     * Свои сообщения берутся по senderId текущего аккаунта, а не из payload: в группе
     * событие рассылается всем участникам, и автор там у каждого свой.
     *
     * Граница — время отправки, а не id: прочтение накрывает всю историю до курсора,
     * а не одно сообщение, и по той же границе выставляются галочки.
     */
    suspend fun apply(payload: ReadMessagePayload) {
        if (payload.userId <= 0 || payload.time <= 0) return
        
        val myId = userRepository.getMe().firstOrNull()?.id ?: return
        
        /* Своё же прочтение с другого устройства в списке просмотров ни к чему. */
        if (payload.userId == myId) return
        
        val messageIds = when {
            payload.sendTime > 0 -> messageDao.getOwnMessageIdsUpTo(
                chatId = payload.chatId, myId = myId, upToSendTime = payload.sendTime
            )
            
            /* Старое событие без границы по времени — только указанное сообщение. */
            payload.messageId > 0 -> listOf(payload.messageId)
            
            else -> emptyList()
        }
        
        if (messageIds.isEmpty()) return
        
        /*
         * Имя остаётся пустым: в событии его нет, а ChatItemMapper умеет подставлять
         * его по userId — из кэша имён либо загрузкой профиля.
         */
        readInfoCache.addReader(
            messageIds = messageIds, reader = MessageReadInfo(
                userId = payload.userId,
                firstName = "",
                lastName = null,
                readAt = payload.time
            )
        )
    }
}
