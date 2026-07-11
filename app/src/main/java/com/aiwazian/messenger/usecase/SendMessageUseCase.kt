/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.usecase

import com.aiwazian.messenger.repository.ChatRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(chatId: Long, message: String, tempId: Long? = null) {
        val result = if (tempId != null) {
            chatRepository.sendMessage(chatId, message, tempId)
        } else {
            chatRepository.sendMessage(chatId, message)
        }
        
        val localChat = chatRepository.getById(chatId).firstOrNull()
        
        if (result.isSuccess && localChat == null) {
            chatRepository.fetchChatByIdFromServer(chatId)
        }
    }
}
