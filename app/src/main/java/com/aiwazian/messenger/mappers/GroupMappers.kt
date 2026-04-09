/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.database.entity.GroupEntity
import com.aiwazian.messenger.domain.Group
import com.aiwazian.messenger.enums.GroupType
import com.aiwazian.messenger.network.dto.GroupResponseDto

fun GroupResponseDto.toDomain(): Group = Group(
    id = id.toLongOrNull() ?: 0L,
    ownerId = ownerId?.toLong(),
    name = name,
    bio = bio,
    username = username,
    groupType = groupType,
    members = membersCount ?: 0
)

fun GroupEntity.toDomain(): Group = Group(
    id = id,
    ownerId = ownerId,
    name = name,
    bio = bio,
    username = username,
    groupType = GroupType.fromOrdinal(groupType),
    members = members
)

fun Group.toEntity(): GroupEntity = GroupEntity(
    id = id,
    name = name,
    bio = bio,
    username = username,
    ownerId = ownerId,
    groupType = groupType.ordinal,
    members = members
)

fun GroupEntity.toGroup(): Group = Group(
    id = id,
    name = name,
    bio = bio,
    username = username,
    ownerId = ownerId,
    groupType = GroupType.fromOrdinal(groupType),
    members = members
)
