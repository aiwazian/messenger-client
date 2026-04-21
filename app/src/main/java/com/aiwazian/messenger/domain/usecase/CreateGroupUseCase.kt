/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain.usecase

import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.repository.GroupRepository
import javax.inject.Inject

class CreateGroupUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(name: String, bio: String): Result<Long> {
        val result = groupRepository.create(name, bio)
        result.onSuccess {
            chatRepository.refreshChats()
        }
        return result
    }
}
