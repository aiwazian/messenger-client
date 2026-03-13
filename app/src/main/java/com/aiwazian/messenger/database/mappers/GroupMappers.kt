package com.aiwazian.messenger.database.mappers

import com.aiwazian.messenger.data.GroupInfo
import com.aiwazian.messenger.database.entity.GroupEntity

fun GroupInfo.toEntity(): GroupEntity {
    return GroupEntity(
        id = this.id,
        name = this.name,
        bio = this.bio,
        ownerId = this.ownerId,
        members = this.members
    )
}

fun GroupEntity.toGroup(): GroupInfo {
    return GroupInfo(
        id = this.id,
        name = this.name,
        bio = this.bio,
        ownerId = this.ownerId,
        members = this.members
    )
}