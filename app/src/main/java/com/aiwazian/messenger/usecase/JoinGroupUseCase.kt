/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.usecase

import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.repository.GroupRepository
import javax.inject.Inject

class JoinGroupUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(groupId: Long): Result<Unit> {
        val result = groupRepository.join(groupId)
        
        if (result.isSuccess) {
            chatRepository.refreshChats()
        }
        
        return result
    }
}