/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.database.entity.GroupEntity
import com.aiwazian.messenger.domain.Group
import com.aiwazian.messenger.network.dto.GroupResponseDto

fun GroupResponseDto.toDomain(): Group = Group(
    id = this.id.toLongOrNull() ?: 0L,
    ownerId = this.ownerId?.toLong(),
    name = this.name,
    bio = this.bio ?: "",
    members = this.membersCount ?: 0
)

fun GroupEntity.toDomain(): Group = Group(
    id = this.id,
    ownerId = this.ownerId,
    name = this.name,
    bio = this.bio,
    members = this.members
)

fun Group.toEntity(): GroupEntity {
    return GroupEntity(
        id = this.id,
        name = this.name,
        bio = this.bio,
        ownerId = this.ownerId,
        members = this.members
    )
}

fun GroupEntity.toGroup(): Group {
    return Group(
        id = this.id,
        name = this.name,
        bio = this.bio,
        ownerId = this.ownerId,
        members = this.members
    )
}