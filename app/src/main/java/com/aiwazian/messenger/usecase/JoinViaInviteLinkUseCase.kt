/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.usecase

import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.repository.InviteLinkRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class JoinViaInviteLinkUseCase @Inject constructor(
    private val inviteLinkRepository: InviteLinkRepository,
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(code: String, chatId: Long): Result<Unit> {
        val result = inviteLinkRepository.joinViaInviteCode(code)
        
        if (result.isSuccess) {
            val existingChat = chatRepository.getById(chatId).firstOrNull()
            if (existingChat == null) {
                chatRepository.fetchChatByIdFromServer(chatId)
            }
        }
        
        return result
    }
}
