/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.usecase

import com.aiwazian.messenger.domain.InviteLinkInfo
import com.aiwazian.messenger.repository.InviteLinkRepository
import javax.inject.Inject

class CheckInviteLinkUseCase @Inject constructor(
    private val inviteLinkRepository: InviteLinkRepository
) {
    suspend operator fun invoke(code: String): Result<InviteLinkInfo> {
        return inviteLinkRepository.getInviteLinkInfo(code)
    }
}
