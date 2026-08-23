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

/**
 * Отправка текстового сообщения.
 *
 * Попыток столько, сколько понадобится: раньше любая сетевая ошибка сразу
 * переводила сообщение в статус «ошибка», и повтор оставался ручным.
 *
 * Цикл живёт в скоупе приложения, поэтому уход с экрана и сворачивание его не
 * обрывают: вызывающая сторона лишь ждёт результат. Остановить отправку можно
 * через [cancel] либо [com.aiwazian.messenger.utils.MessageSendQueue.cancel], а
 * ещё её останавливает исчезновение самого сообщения: «Отменить отправку»
 * удаляет его из чата, и повторять после этого нечего.
 *
 * Смерть процесса цикл не переживает, и записи на диске здесь больше нет: текст
 * целиком лежит в самом сообщении, поэтому оборванную отправку поднимает
 * [com.aiwazian.messenger.utils.FailedSendRetrier] при следующем открытии чата.
 * Файлы так не восстановить, им запись нужна — см. [SendMessageWithFilesUseCase].
 */
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
        // Первая попытка идёт всегда: локальное сообщение создаёт сама отправка,
        // и до неё в базе его ещё нет. Дальше пустота на его месте означает
        // отмену — сообщение убрали из чата.
        var attempted = false
        
        val result = RetryPolicy.retryForever(
            operation = "sendText#$localId",
            isPermanent = { it is SendCancelledException }
        ) {
            if (attempted && isCancelled(localId)) {
                return@retryForever Result.failure<Message>(SendCancelledException(localId))
            }
            
            attempted = true
            
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
        }.onFailure { error ->
            if (error is SendCancelledException) {
                Log.i(TAG, "Sending of #$localId dropped: message is gone")
            } else {
                chatRepository.updateMessageStatus(localId, MessageStatus.ERROR)
            }
        }
        
        return result
    }
    
    /**
     * Сбой чтения базы считаем за «сообщение на месте»: оборвать из-за него
     * живую отправку хуже, чем сделать лишнюю попытку.
     */
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
