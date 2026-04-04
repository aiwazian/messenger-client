/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.domain.InviteLink
import com.aiwazian.messenger.network.dto.InviteLinkResponseDto

fun InviteLinkResponseDto.toDomain(): InviteLink = InviteLink(
    id = this.id.toLongOrNull() ?: 0L,
    chatId = this.chatId.toLongOrNull() ?: 0L,
    code = this.code,
    link = this.link,
    expiresAt = this.expiresAt,
    maxUses = this.maxUses,
    uses = this.uses
)
