/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.usecase

import com.aiwazian.messenger.domain.MessageReplyPreview
import com.aiwazian.messenger.repository.ChatRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(
        chatId: Long,
        message: String,
        tempId: Long? = null,
        replyTo: MessageReplyPreview? = null
    ) {
        val result = if (tempId != null) {
            chatRepository.sendMessage(chatId, message, tempId, replyTo)
        } else {
            chatRepository.sendMessage(chatId, message, replyTo = replyTo)
        }
        
        val localChat = chatRepository.getById(chatId).firstOrNull()
        
        if (result.isSuccess && localChat == null) {
            chatRepository.fetchChatByIdFromServer(chatId)
        }
    }
}
