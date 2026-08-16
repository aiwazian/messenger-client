/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.usecase

import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.domain.MessageReplyPreview
import com.aiwazian.messenger.enums.MessageStatus
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.utils.RetryPolicy
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * Отправка текстового сообщения.
 *
 * Попыток столько, сколько понадобится: раньше любая сетевая ошибка сразу
 * переводила сообщение в статус «ошибка», и повтор оставался ручным.
 *
 * Цикл живёт в скоупе вызывающей стороны, поэтому отмена отправки
 * по-прежнему делается отменой корутины. Чтобы повторы не обрывались при
 * уходе с экрана, вызывайте через [com.aiwazian.messenger.utils.MessageSendQueue].
 */
class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(
        chatId: Long,
        message: String,
        tempId: Long? = null,
        replyTo: MessageReplyPreview? = null
    ): Result<Message> {
        val localId = tempId ?: -System.currentTimeMillis()
        
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
