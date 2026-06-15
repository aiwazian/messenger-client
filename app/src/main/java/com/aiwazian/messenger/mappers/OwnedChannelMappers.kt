/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.domain.OwnedChannel
import com.aiwazian.messenger.network.dto.OwnedChannelDto

fun OwnedChannelDto.toDomain(): OwnedChannel = OwnedChannel(
    id = id.toLongOrNull() ?: 0L,
    name = name,
    subscribers = subscribers,
    avatar = avatar?.toDomain()
)
