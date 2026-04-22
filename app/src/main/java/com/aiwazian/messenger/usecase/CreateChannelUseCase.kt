/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.usecase

import com.aiwazian.messenger.repository.ChannelRepository
import com.aiwazian.messenger.repository.ChatRepository
import javax.inject.Inject

class CreateChannelUseCase @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(name: String, bio: String): Result<Long> {
        val result = channelRepository.create(name, bio)
        result.onSuccess {
            chatRepository.refreshChats()
        }
        return result
    }
}
