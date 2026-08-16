/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.net.Uri
import android.util.Log
import com.aiwazian.messenger.di.ApplicationScope
import com.aiwazian.messenger.domain.MessageReplyPreview
import com.aiwazian.messenger.enums.MessageStatus
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.usecase.SendMessageUseCase
import com.aiwazian.messenger.usecase.SendMessageWithFilesUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Очередь исходящих сообщений: текст, файлы и голосовые.
 *
 * Отправка запускается в скоупе приложения, а не экрана: раньше она жила в
 * viewModelScope, поэтому выход из чата обрывал её на полпути. Попытки не
 * ограничены (см. [RetryPolicy]), поэтому статус ERROR остаётся только для
 * случаев, когда повторять действительно нечего — например, хранилище отказалось
 * принимать файл.
 */
@Singleton
class MessageSendQueue @Inject constructor(
    @param:ApplicationScope private val appScope: CoroutineScope,
    private val chatRepository: ChatRepository,
    private val sendMessageUseCase: SendMessageUseCase,
    private val sendMessageWithFilesUseCase: SendMessageWithFilesUseCase
) {
    
    private val jobs = ConcurrentHashMap<Long, Job>()
    
    /** Идёт ли отправка прямо сейчас: чтобы «повторить» не удвоило её. */
    fun isPending(tempId: Long): Boolean = jobs[tempId]?.isActive == true
    
    /**
     * Текстовое сообщение. Повторяется, пока сервер его не примет.
     *
     * @return локальный id сообщения, по которому отправку можно отменить.
     */
    fun enqueueText(
        chatId: Long,
        text: String,
        tempId: Long = nextTempId(),
        replyTo: MessageReplyPreview? = null
    ): Long {
        enqueue(tempId) {
            RetryPolicy.retryForever("sendText#$tempId") {
                sendMessageUseCase(
                    chatId = chatId,
                    message = text,
                    tempId = tempId,
                    replyTo = replyTo,
                    markErrorOnFailure = false
                )
            }.onFailure { markAsError(tempId, it) }
        }
        
        return tempId
    }

    /**
     * Файлы и голосовые.
     *
     * Повторы живут внутри use case: там они не пересоздают локальное сообщение
     * и не загружают заново то, что сервер уже принял.
     */
    fun enqueueFiles(
        chatId: Long,
        uris: List<Uri>,
        text: String? = null,
        tempId: Long = nextTempId(),
        replyTo: MessageReplyPreview? = null
    ): Long {
        enqueue(tempId) {
            sendMessageWithFilesUseCase(
                chatId = chatId,
                uris = uris,
                text = text,
                tempId = tempId,
                replyTo = replyTo
            )
        }
        
        return tempId
    }
    
    /** Пользователь сам убрал сообщение из чата. */
    fun cancel(tempId: Long) {
        jobs.remove(tempId)?.cancel()
    }
    
    private fun enqueue(tempId: Long, block: suspend () -> Unit) {
        jobs.remove(tempId)?.cancel()
        
        val job = appScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                markAsError(tempId, e)
            }
        }
        
        jobs[tempId] = job
        job.invokeOnCompletion { jobs.remove(tempId, job) }
    }
    
    private suspend fun markAsError(tempId: Long, error: Throwable) {
        Log.e(TAG, "Message #$tempId will not be retried", error)
        chatRepository.updateMessageStatus(tempId, MessageStatus.ERROR)
    }
    
    private fun nextTempId(): Long = -System.currentTimeMillis()
    
    private companion object {
        const val TAG = "MessageSendQueue"
    }
}
