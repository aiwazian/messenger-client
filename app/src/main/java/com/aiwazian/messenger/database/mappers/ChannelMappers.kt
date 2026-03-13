package com.aiwazian.messenger.database.mappers

import com.aiwazian.messenger.data.ChannelInfo
import com.aiwazian.messenger.database.entity.ChannelEntity

fun ChannelInfo.toEntity(): ChannelEntity {
    return ChannelEntity(
        id = this.id,
        name = this.name,
        bio = this.bio,
        ownerId = this.ownerId,
        subscribers = this.subscribers,
        removedUser = this.removedUser,
        channelType = this.channelType,
        publicLink = this.publicLink,
        isSubscribed = this.isSubscribed
    )
}

fun ChannelEntity.toChannel(): ChannelInfo {
    return ChannelInfo(
        id = this.id,
        name = this.name,
        bio = this.bio,
        ownerId = this.ownerId,
        subscribers = this.subscribers,
        removedUser = this.removedUser,
        channelType = this.channelType,
        publicLink = this.publicLink,
        isSubscribed = this.isSubscribed
    )
}