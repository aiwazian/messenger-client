/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.database.entity.AvatarEntity
import com.aiwazian.messenger.database.entity.ChannelEntity
import com.aiwazian.messenger.domain.Avatar
import com.aiwazian.messenger.domain.Channel
import com.aiwazian.messenger.network.dto.AvatarDto
import com.aiwazian.messenger.network.dto.ChannelResponseDto

fun ChannelResponseDto.toDomain() = Channel(
    id = id,
    ownerId = ownerId,
    name = name,
    bio = bio,
    subscribers = subscribers,
    removedUsers = removedUsers,
    channelType = channelType,
    username = username,
    isSubscribed = isSubscribed,
    noCopy = noCopy,
    avatars = avatars.map { it.toDomain() }
)

fun ChannelEntity.toDomain(avatars: List<Avatar> = emptyList()) = Channel(
    id = id,
    ownerId = ownerId,
    name = name,
    bio = bio,
    subscribers = subscribers,
    removedUsers = removedUsers,
    channelType = channelType,
    username = username,
    isSubscribed = isSubscribed,
    noCopy = noCopy,
    avatars = avatars
)

fun Channel.toEntity() = ChannelEntity(
    id = id,
    name = name,
    bio = bio,
    ownerId = ownerId,
    subscribers = subscribers,
    removedUsers = removedUsers,
    channelType = channelType,
    username = username,
    isSubscribed = isSubscribed,
    noCopy = noCopy
)

fun AvatarDto.toChannelEntity(channelId: Long) = AvatarEntity(
    fileId = fileId,
    channelId = channelId,
    sortOrder = sortOrder
)
