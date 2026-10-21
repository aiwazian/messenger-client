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

@Singleton
class ReadReceiptApplier @Inject constructor(
    private val messageDao: MessageDao,
    private val readInfoCache: MessageReadInfoCache,
    private val userRepository: UserRepository
) {
 
    suspend fun apply(payload: ReadMessagePayload) {
        if (payload.userId <= 0 || payload.time <= 0) return
        
        val myId = userRepository.getMe().firstOrNull()?.id ?: return
        
        if (payload.userId == myId) return
        
        val messageIds = when {
            payload.sendTime > 0 -> messageDao.getOwnMessageIdsUpTo(
                chatId = payload.chatId, myId = myId, upToSendTime = payload.sendTime
            )
            
            payload.messageId > 0 -> listOf(payload.messageId)
            
            else -> emptyList()
        }
        
        if (messageIds.isEmpty()) return
        
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
