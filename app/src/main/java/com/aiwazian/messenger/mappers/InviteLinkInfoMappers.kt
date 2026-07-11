/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.domain.InviteLinkInfo
import com.aiwazian.messenger.network.dto.InviteLinkInfoDto

fun InviteLinkInfoDto.toDomain(): InviteLinkInfo {
    return InviteLinkInfo(
        chatId = chatId,
        name = name,
        description = description,
        membersCount = membersCount,
        isBanned = isBanned,
        isJoined = isJoined,
        requireApproval = requireApproval ?: false
    )
}
