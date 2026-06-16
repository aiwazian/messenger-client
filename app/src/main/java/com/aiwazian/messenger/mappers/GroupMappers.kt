/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.database.entity.AvatarEntity
import com.aiwazian.messenger.database.entity.GroupEntity
import com.aiwazian.messenger.domain.Avatar
import com.aiwazian.messenger.domain.Group
import com.aiwazian.messenger.network.dto.AvatarDto
import com.aiwazian.messenger.network.dto.GroupResponseDto

fun GroupResponseDto.toDomain(): Group = Group(
    id = id,
    ownerId = ownerId,
    name = name,
    bio = bio,
    username = username,
    groupType = groupType,
    members = membersCount ?: 0,
    removedUsers = removedUsers,
    isMember = isMember,
    avatars = avatars.map { it.toDomain() }
)

fun GroupEntity.toDomain(avatars: List<Avatar> = emptyList()): Group = Group(
    id = id,
    ownerId = ownerId,
    name = name,
    bio = bio,
    username = username,
    groupType = groupType,
    members = members,
    removedUsers = removedUsers,
    isMember = isMember,
    avatars = avatars
)

fun Group.toEntity() = GroupEntity(
    id = id,
    name = name,
    bio = bio,
    username = username,
    ownerId = ownerId,
    groupType = groupType,
    members = members,
    removedUsers = removedUsers,
    isMember = isMember
)

fun AvatarDto.toGroupEntity(groupId: Long) = AvatarEntity(
    fileId = fileId,
    groupId = groupId,
    sortOrder = sortOrder
)
