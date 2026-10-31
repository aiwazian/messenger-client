/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.usecase

import android.util.Log
import com.aiwazian.messenger.database.dao.MessageDao
import com.aiwazian.messenger.di.ApplicationScope
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.domain.MessageReplyPreview
import com.aiwazian.messenger.domain.SendCancelledException
import com.aiwazian.messenger.enums.MessageStatus
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.utils.RetryPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SendMessageUseCase @Inject constructor(
    @param:ApplicationScope private val appScope: CoroutineScope,
    private val chatRepository: ChatRepository,
    private val messageDao: MessageDao
) {
    private val running = ConcurrentHashMap<Long, Deferred<Result<Message>>>()
    
    suspend operator fun invoke(
        chatId: Long,
        message: String,
        tempId: Long? = null,
        replyTo: MessageReplyPreview? = null,
        silent: Boolean = false,
        scheduledAt: Long? = null
    ): Result<Message> {
        val localId = tempId ?: -System.currentTimeMillis()

        val sending = appScope.async(start = CoroutineStart.LAZY) {
            send(chatId, message, localId, replyTo, silent, scheduledAt)
        }

        running.put(localId, sending)?.cancel()
        sending.invokeOnCompletion { running.remove(localId, sending) }
        sending.start()

        return sending.await()
    }

    fun cancel(tempId: Long) {
        running.remove(tempId)?.cancel()
    }

    fun isRunning(tempId: Long): Boolean = running[tempId]?.isActive == true

    private suspend fun send(
        chatId: Long,
        message: String,
        localId: Long,
        replyTo: MessageReplyPreview?,
        silent: Boolean,
        scheduledAt: Long?
    ): Result<Message> {
        var attempted = false

        val result = RetryPolicy.retryForever(
            operation = "sendText#$localId",
            isPermanent = { it is SendCancelledException }
        ) {
            if (attempted && isCancelled(localId)) {
                return@retryForever Result.failure(SendCancelledException(localId))
            }

            attempted = true

            val attempt = chatRepository.sendMessage(chatId, message, localId, replyTo, silent, scheduledAt)

            if (attempt.isFailure) {
                chatRepository.updateMessageStatus(localId, MessageStatus.SENDING)
            }

            attempt
        }
        
        result.onSuccess {
            val localChat = chatRepository.getById(chatId).firstOrNull()
            
            if (localChat == null) {
                chatRepository.fetchChatByIdFromServer(chatId)
            }
        }.onFailure { error ->
            if (error is SendCancelledException) {
                Log.i(TAG, "Sending of #$localId dropped: message is gone")
            } else {
                chatRepository.updateMessageStatus(localId, MessageStatus.ERROR)
            }
        }
        
        return result
    }

    private suspend fun isCancelled(tempId: Long): Boolean = try {
        messageDao.getMessageById(tempId) == null
    } catch (e: Exception) {
        Log.e(TAG, "Unable to check message #$tempId", e)
        false
    }
    
    private companion object {
        const val TAG = "SendMessage"
    }
}
