/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.database.entity.UserEntity
import com.aiwazian.messenger.domain.Avatar
import com.aiwazian.messenger.domain.PrivacySettings
import com.aiwazian.messenger.domain.User
import com.aiwazian.messenger.network.dto.PrivacySettingsResponseDto
import com.aiwazian.messenger.network.dto.UpdateUserRequestDto
import com.aiwazian.messenger.network.dto.UserResponseDto

fun UserResponseDto.toDomain(): User = User(
    id = id,
    firstName = firstName ?: "",
    lastName = lastName,
    username = username,
    bio = bio,
    dateOfBirth = dateOfBirth,
    lastSeen = lastSeen,
    avatars = avatars.map { Avatar(it.fileId, it.sortOrder) }
)

fun UserResponseDto.toEntity(): UserEntity = UserEntity(
    id = id,
    firstName = firstName ?: "",
    lastName = lastName,
    username = username,
    bio = bio,
    dateOfBirth = dateOfBirth,
    lastSeen = lastSeen
)

fun User.toUpdateRequest(): UpdateUserRequestDto = UpdateUserRequestDto(
    firstName = firstName,
    lastName = lastName,
    username = username,
    bio = bio,
    dateOfBirth = dateOfBirth
)

fun PrivacySettingsResponseDto.toDomain() = PrivacySettings(
    lastSeen = lastSeen,
    messages = messages,
    bio = bio,
    dateOfBirth = dateOfBirth,
    invites = invites
)

fun UserEntity.toDomain(): User = User(
    id = id,
    firstName = firstName,
    lastName = lastName,
    username = username,
    bio = bio,
    dateOfBirth = dateOfBirth,
    lastSeen = lastSeen,
    avatars = emptyList()
)

fun User.toEntity(): UserEntity = UserEntity(
    id = id,
    firstName = firstName,
    lastName = lastName,
    username = username,
    bio = bio,
    dateOfBirth = dateOfBirth,
    lastSeen = lastSeen
)
