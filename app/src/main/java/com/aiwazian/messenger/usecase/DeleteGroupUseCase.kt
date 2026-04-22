/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.usecase

import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.repository.GroupRepository
import javax.inject.Inject

class DeleteGroupUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(chatId: Long): Boolean {
        val success = groupRepository.delete(chatId).isSuccess
        if (success) {
            chatRepository.deleteChat(chatId)
        }
        return success
    }
}
