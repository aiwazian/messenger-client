/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain.usecase

import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.repository.ChatRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(chatId: Long, message: String): Message? {
        val result = chatRepository.sendMessage(chatId, message)
        
        val localChat = chatRepository.getById(chatId).firstOrNull()
        
        if (result != null && localChat == null) {
            chatRepository.refreshChats()
        }
        
        return result
    }
}
