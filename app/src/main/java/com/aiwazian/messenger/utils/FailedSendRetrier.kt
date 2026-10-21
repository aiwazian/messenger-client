/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.util.Log
import com.aiwazian.messenger.database.dao.MessageDao
import com.aiwazian.messenger.di.ApplicationScope
import com.aiwazian.messenger.enums.MessageStatus
import com.aiwazian.messenger.mappers.toDomain
import com.aiwazian.messenger.usecase.SendMessageUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FailedSendRetrier @Inject constructor(
    @param:ApplicationScope private val appScope: CoroutineScope,
    private val messageDao: MessageDao,
    private val messageSendQueue: MessageSendQueue,
    private val sendMessageUseCase: SendMessageUseCase
) {
    
    fun start() {
        appScope.launch {
            ActiveChatTracker.activeChatId.filterNotNull().collect { chatId ->
                retryIn(chatId)
            }
        }
    }
    
    private suspend fun retryIn(chatId: Long) {
        val unsent = try {
            unsentMessages(chatId)
        } catch (e: Exception) {
            Log.e(TAG, "Reading unsent messages of chat $chatId failed", e)
            return
        }
        
        unsent.forEach { row ->
            val message = row.message
            
            if (message.status != MessageStatus.SENDING && message.status != MessageStatus.ERROR) {
                return@forEach
            }
            
            if (messageSendQueue.isPending(message.id) || sendMessageUseCase.isRunning(message.id)) {
                return@forEach
            }
            
            if (row.attachments.isNotEmpty()) {
                return@forEach
            }
            
            val text = message.text
            
            if (text.isNullOrBlank()) {
                return@forEach
            }
            
            Log.d(TAG, "Resending text message #${message.id}")
            
            messageSendQueue.enqueueText(
                chatId = message.chatId,
                text = text,
                tempId = message.id,
                replyTo = message.toDomain().replyTo
            )
        }
    }

    private suspend fun unsentMessages(chatId: Long) = messageDao.getChatMessagesWindow(
        chatId = chatId,
        fromId = Long.MAX_VALUE,
        toId = Long.MIN_VALUE,
        includePending = 1
    ).first()
    
    private companion object {
        const val TAG = "FailedSendRetrier"
    }
}
