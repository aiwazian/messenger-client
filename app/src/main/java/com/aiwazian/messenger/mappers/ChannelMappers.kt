/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.database.entity.ChannelEntity
import com.aiwazian.messenger.domain.Channel
import com.aiwazian.messenger.enums.ChannelType
import com.aiwazian.messenger.network.dto.ChannelResponseDto

fun ChannelResponseDto.toDomain() = Channel(
    id = id.toLongOrNull() ?: 0L,
    ownerId = ownerId?.toLongOrNull() ?: 0L,
    name = name,
    bio = bio ?: "",
    subscribers = subscribers?.toIntOrNull() ?: 0,
    removedUser = removedUser?.toIntOrNull() ?: 0,
    channelType = channelType,
    username = username,
    isSubscribed = isSubscribed
)

fun ChannelEntity.toDomain() = Channel(
    id = id,
    ownerId = ownerId,
    name = name,
    bio = bio,
    subscribers = subscribers,
    removedUser = removedUser,
    channelType = ChannelType.fromOrdinal(channelType),
    username = username,
    isSubscribed = isSubscribed
)

fun Channel.toEntity() = ChannelEntity(
    id = id,
    name = name,
    bio = bio,
    ownerId = ownerId,
    subscribers = subscribers,
    removedUser = removedUser,
    channelType = channelType.ordinal,
    username = username,
    isSubscribed = isSubscribed
)
