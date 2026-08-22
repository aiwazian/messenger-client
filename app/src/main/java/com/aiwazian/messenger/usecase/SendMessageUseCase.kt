/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.usecase

import com.aiwazian.messenger.di.ApplicationScope
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.domain.MessageReplyPreview
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

/**
 * Отправка текстового сообщения.
 *
 * Попыток столько, сколько понадобится: раньше любая сетевая ошибка сразу
 * переводила сообщение в статус «ошибка», и повтор оставался ручным.
 *
 * Цикл живёт в скоупе приложения, поэтому уход с экрана и сворачивание его не
 * обрывают: вызывающая сторона лишь ждёт результат. Остановить отправку можно
 * через [cancel] либо [com.aiwazian.messenger.utils.MessageSendQueue.cancel].
 *
 * Смерть процесса цикл не переживает, и записи на диске здесь больше нет: текст
 * целиком лежит в самом сообщении, поэтому оборванную отправку поднимает
 * [com.aiwazian.messenger.utils.FailedSendRetrier] при следующем открытии чата.
 * Файлы так не восстановить, им запись нужна — см. [SendMessageWithFilesUseCase].
 */
@Singleton
class SendMessageUseCase @Inject constructor(
    @param:ApplicationScope private val appScope: CoroutineScope,
    private val chatRepository: ChatRepository
) {
    private val running = ConcurrentHashMap<Long, Deferred<Result<Message>>>()
    
    suspend operator fun invoke(
        chatId: Long,
        message: String,
        tempId: Long? = null,
        replyTo: MessageReplyPreview? = null
    ): Result<Message> {
        val localId = tempId ?: -System.currentTimeMillis()
        
        val sending = appScope.async(start = CoroutineStart.LAZY) {
            send(chatId, message, localId, replyTo)
        }
        
        running.put(localId, sending)?.cancel()
        sending.invokeOnCompletion { running.remove(localId, sending) }
        sending.start()
        
        return sending.await()
    }
    
    /** Останавливает отправку, которая продолжается в скоупе приложения. */
    fun cancel(tempId: Long) {
        running.remove(tempId)?.cancel()
    }
    
    /**
     * Идёт ли отправка прямо сейчас.
     *
     * Нужно тем, кто досылает зависшие сообщения со стороны: сообщение может
     * долго висеть «отправляется» с работающим циклом повторов внутри, и
     * повторная постановка в очередь только оборвала бы его на полпути.
     */
    fun isRunning(tempId: Long): Boolean = running[tempId]?.isActive == true
    
    private suspend fun send(
        chatId: Long,
        message: String,
        localId: Long,
        replyTo: MessageReplyPreview?
    ): Result<Message> {
        val result = RetryPolicy.retryForever("sendText#$localId") {
            val attempt = chatRepository.sendMessage(chatId, message, localId, replyTo)
            
            if (attempt.isFailure) {
                // Репозиторий пометил сообщение ошибочным, но попытки ещё не
                // закончились: в чате оно обязано оставаться «отправляется», иначе
                // восклицательный знак мигал бы на каждой неудачной попытке.
                chatRepository.updateMessageStatus(localId, MessageStatus.SENDING)
            }
            
            attempt
        }
        
        result.onSuccess {
            val localChat = chatRepository.getById(chatId).firstOrNull()
            
            if (localChat == null) {
                chatRepository.fetchChatByIdFromServer(chatId)
            }
        }.onFailure {
            chatRepository.updateMessageStatus(localId, MessageStatus.ERROR)
        }
        
        return result
    }
}
