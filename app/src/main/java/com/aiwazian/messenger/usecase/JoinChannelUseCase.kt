/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.usecase

import com.aiwazian.messenger.repository.ChannelRepository
import com.aiwazian.messenger.repository.ChatRepository
import javax.inject.Inject

class JoinChannelUseCase @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(channelId: Long): Result<Unit> {
        val result = channelRepository.join(channelId)
        
        if (result.isSuccess) {
            chatRepository.refreshChats()
        }
        
        return result
    }
}