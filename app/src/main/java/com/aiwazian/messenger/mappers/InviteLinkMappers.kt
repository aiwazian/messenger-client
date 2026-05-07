/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.domain.InviteLink
import com.aiwazian.messenger.network.dto.InviteLinkResponseDto
import okhttp3.internal.toLongOrDefault

fun InviteLinkResponseDto.toDomain() = InviteLink(
    id = id,
    chatId = chatId,
    code = code,
    link = link,
    expiresAt = expiresAt,
    maxUses = maxUses,
    uses = uses
)
