/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.domain.InviteLinkInfo
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.network.dto.InviteLinkInfoDto

fun InviteLinkInfoDto.toDomain(): InviteLinkInfo {
    return InviteLinkInfo(
        chatId = chatId.toLong(),
        name = name,
        description = description,
        membersCount = membersCount,
        isBanned = isBanned,
        isJoined = isJoined,
        type = ChatType.valueOf(type)
    )
}
