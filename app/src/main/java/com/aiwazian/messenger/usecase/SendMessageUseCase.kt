/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.usecase

import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.domain.MessageReplyPreview
import com.aiwazian.messenger.enums.MessageStatus
import com.aiwazian.messenger.repository.ChatRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    /**
     * @param markErrorOnFailure оставлять ли сообщению статус «ошибка».
     * Отправка с повторами передаёт false: между попытками сообщение обязано
     * выглядеть как «отправляется», иначе в чате мигал бы восклицательный знак.
     */
    suspend operator fun invoke(
        chatId: Long,
        message: String,
        tempId: Long? = null,
        replyTo: MessageReplyPreview? = null,
        markErrorOnFailure: Boolean = true
    ): Result<Message> {
        val result = if (tempId != null) {
            chatRepository.sendMessage(chatId, message, tempId, replyTo)
        } else {
            chatRepository.sendMessage(chatId, message, replyTo = replyTo)
        }
        
        if (result.isFailure && !markErrorOnFailure && tempId != null) {
            chatRepository.updateMessageStatus(tempId, MessageStatus.SENDING)
        }
        
        val localChat = chatRepository.getById(chatId).firstOrNull()
        
        if (result.isSuccess && localChat == null) {
            chatRepository.fetchChatByIdFromServer(chatId)
        }
        
        return result
    }
}
