/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.domain.InviteLink
import com.aiwazian.messenger.network.dto.InviteLinkResponseDto

fun InviteLinkResponseDto.toDomain() = InviteLink(
    id = id,
    code = code,
    expiresAt = expiresAt,
    maxUses = maxUses,
    uses = uses
)
