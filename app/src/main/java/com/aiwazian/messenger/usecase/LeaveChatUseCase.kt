/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.usecase

import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.repository.ChannelRepository
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.repository.GroupRepository
import javax.inject.Inject

class LeaveChatUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
    private val channelRepository: ChannelRepository,
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(chatId: Long): Result<Unit> {
        return when (ChatType.fromId(chatId)) {
            ChatType.GROUP -> groupRepository.leave(chatId)
            ChatType.CHANNEL -> channelRepository.leave(chatId)
            else -> Result.failure(Exception("Invalid chat type"))
        }.onSuccess {
            chatRepository.deleteLocalChat(chatId)
        }
    }
}
