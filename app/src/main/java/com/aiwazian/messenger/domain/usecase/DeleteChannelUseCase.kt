package com.aiwazian.messenger.domain.usecase

import com.aiwazian.messenger.repository.ChannelRepository
import com.aiwazian.messenger.repository.ChatRepository
import javax.inject.Inject

class DeleteChannelUseCase @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(chatId: Long): Boolean {
        val success = channelRepository.delete(chatId).isSuccess
        if (success) {
            chatRepository.deleteChat(chatId)
        }
        return success
    }
}
