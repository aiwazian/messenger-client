/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.network.dto.SessionResponseDto
import com.aiwazian.messenger.domain.Session

fun SessionResponseDto.toDomain(): Session = Session(
    id = id,
    userId = userId,
    osName = osName,
    osVersion = osVersion,
    deviceModel = deviceModel,
    createdAt = createdAt.toLong()
)
