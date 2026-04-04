/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.database.entity.ChannelEntity
import com.aiwazian.messenger.domain.Channel
import com.aiwazian.messenger.enums.ChannelType
import com.aiwazian.messenger.network.dto.ChannelResponseDto

fun ChannelResponseDto.toDomain(): Channel = Channel(
    id = id.toLongOrNull() ?: 0L,
    ownerId = ownerId?.toLongOrNull() ?: 0L,
    name = name,
    bio = bio ?: "",
    subscribers = subscribers?.toIntOrNull() ?: 0,
    removedUser = removedUser?.toIntOrNull() ?: 0,
    channelType = channelType,
    username = username,
    inviteLink = inviteLink,
    isSubscribed = isSubscribed
)

fun ChannelEntity.toDomain(): Channel = Channel(
    id = id,
    ownerId = ownerId,
    name = name,
    bio = bio,
    subscribers = subscribers,
    removedUser = removedUser,
    channelType = ChannelType.fromOrdinal(channelType),
    username = username,
    inviteLink = inviteLink,
    isSubscribed = isSubscribed
)

fun Channel.toEntity(): ChannelEntity {
    return ChannelEntity(
        id = id,
        name = name,
        bio = bio,
        ownerId = ownerId,
        subscribers = subscribers,
        removedUser = removedUser,
        channelType = channelType.ordinal,
        username = username,
        inviteLink = inviteLink,
        isSubscribed = isSubscribed
    )
}

fun ChannelEntity.toChannel(): Channel {
    return Channel(
        id = id,
        name = name,
        bio = bio,
        ownerId = ownerId,
        subscribers = subscribers,
        removedUser = removedUser,
        channelType = ChannelType.fromOrdinal(channelType),
        username = username,
        isSubscribed = isSubscribed
    )
}
