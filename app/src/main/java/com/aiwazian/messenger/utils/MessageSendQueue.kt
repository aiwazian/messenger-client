/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.net.Uri
import android.util.Log
import com.aiwazian.messenger.di.ApplicationScope
import com.aiwazian.messenger.domain.MessageReplyPreview
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
 * Отправка в скоупе приложения — для экранов, которые закрываются сразу после
 * нажатия «Отправить».
 *
 * Системное «Поделиться» и шторка в профиле исчезают быстрее, чем успевает
 * уйти запрос, а вместе с ними умирал бы viewModelScope и все повторные
 * попытки. Здесь отправка привязана к жизни процесса, а сами повторы живут
 * внутри use case — см. [SendMessageUseCase] и [SendMessageWithFilesUseCase].
 */
@Singleton
class MessageSendQueue @Inject constructor(
    @param:ApplicationScope private val appScope: CoroutineScope,
    private val sendMessageUseCase: SendMessageUseCase,
    private val sendMessageWithFilesUseCase: SendMessageWithFilesUseCase
) {
    
    private val jobs = ConcurrentHashMap<Long, Job>()
    
    /** Идёт ли отправка прямо сейчас: чтобы повторное нажатие не удвоило её. */
    fun isPending(tempId: Long): Boolean = jobs[tempId]?.isActive == true
    
    /**
     * Текстовое сообщение.
     *
     * @return локальный id, по которому отправку можно отменить.
     */
    fun enqueueText(
        chatId: Long,
        text: String,
        tempId: Long = nextTempId(),
        replyTo: MessageReplyPreview? = null
    ): Long {
        enqueue(tempId) {
            sendMessageUseCase(
                chatId = chatId,
                message = text,
                tempId = tempId,
                replyTo = replyTo
            )
        }
        
        return tempId
    }
    
    /** Файлы, фото, видео и голосовые. */
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
                Log.e(TAG, "Sending #$tempId failed", e)
            }
        }
        
        jobs[tempId] = job
        job.invokeOnCompletion { jobs.remove(tempId, job) }
    }
    
    private fun nextTempId(): Long = -System.currentTimeMillis()
    
    private companion object {
        const val TAG = "MessageSendQueue"
    }
}
