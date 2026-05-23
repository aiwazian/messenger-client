/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.usecase

import com.aiwazian.messenger.repository.ChannelRepository
import com.aiwazian.messenger.repository.ChatRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class JoinChannelUseCase @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(channelId: Long): Result<Unit> {
        val result = channelRepository.join(channelId)
        
        if (result.isSuccess) {
            val existingChat = chatRepository.getById(channelId).firstOrNull()
            if (existingChat == null) {
                chatRepository.fetchChatByIdFromServer(channelId)
            }
        }
        
        return result
    }
}