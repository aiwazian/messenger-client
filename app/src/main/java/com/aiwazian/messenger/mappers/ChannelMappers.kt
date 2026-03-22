/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.network.dto.ChannelResponseDto
import com.aiwazian.messenger.domain.Channel
import com.aiwazian.messenger.database.entity.ChannelEntity

fun ChannelResponseDto.toDomain(): Channel = Channel(
    id = this.id.toLongOrNull() ?: 0L,
    ownerId = this.ownerId?.toLongOrNull() ?: 0L,
    name = this.name,
    bio = this.bio ?: "",
    subscribers = this.subscribers?.toIntOrNull() ?: 0,
    removedUser = this.removedUser?.toIntOrNull() ?: 0,
    channelType = this.channelType.ordinal,
    username = this.username,
    inviteLink = this.inviteLink,
    isSubscribed = this.isSubscribed
)

fun ChannelEntity.toDomain(): Channel = Channel(
    id = this.id,
    ownerId = this.ownerId,
    name = this.name,
    bio = this.bio,
    subscribers = this.subscribers,
    removedUser = this.removedUser,
    channelType = this.channelType,
    username = this.username,
    inviteLink = this.inviteLink,
    isSubscribed = this.isSubscribed
)

fun Channel.toEntity(): ChannelEntity {
    return ChannelEntity(
        id = this.id,
        name = this.name,
        bio = this.bio,
        ownerId = this.ownerId,
        subscribers = this.subscribers,
        removedUser = this.removedUser,
        channelType = this.channelType,
        username = this.username,
        inviteLink = inviteLink,
        isSubscribed = this.isSubscribed
    )
}

fun ChannelEntity.toChannel(): Channel {
    return Channel(
        id = this.id,
        name = this.name,
        bio = this.bio,
        ownerId = this.ownerId,
        subscribers = this.subscribers,
        removedUser = this.removedUser,
        channelType = this.channelType,
        username = this.username,
        isSubscribed = this.isSubscribed
    )
}