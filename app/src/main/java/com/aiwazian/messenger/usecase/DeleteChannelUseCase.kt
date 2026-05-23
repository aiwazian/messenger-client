/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.usecase

import com.aiwazian.messenger.repository.ChannelRepository
import com.aiwazian.messenger.repository.ChatRepository
import javax.inject.Inject

class DeleteChannelUseCase @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(chatId: Long): Boolean {
        val success = channelRepository.delete(chatId).onSuccess {
            chatRepository.deleteChat(chatId)
        }
        return success.isSuccess
    }
}
