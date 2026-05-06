/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.database.entity.GroupEntity
import com.aiwazian.messenger.domain.Group
import com.aiwazian.messenger.network.dto.GroupResponseDto

fun GroupResponseDto.toDomain(): Group = Group(
    id = id,
    ownerId = ownerId,
    name = name,
    bio = bio,
    username = username,
    groupType = groupType,
    members = membersCount ?: 0,
    isMember = isMember
)

fun GroupEntity.toDomain(): Group = Group(
    id = id,
    ownerId = ownerId,
    name = name,
    bio = bio,
    username = username,
    groupType = groupType,
    members = members,
    isMember = isMember
)

fun Group.toEntity() = GroupEntity(
    id = id,
    name = name,
    bio = bio,
    username = username,
    ownerId = ownerId,
    groupType = groupType,
    members = members,
    isMember = isMember
)
