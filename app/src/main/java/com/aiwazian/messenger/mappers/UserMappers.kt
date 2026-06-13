/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.mappers

import android.net.Uri
import com.aiwazian.messenger.database.entity.AvatarEntity
import com.aiwazian.messenger.database.entity.UserEntity
import com.aiwazian.messenger.domain.Avatar
import com.aiwazian.messenger.domain.PrivacySettings
import com.aiwazian.messenger.domain.User
import com.aiwazian.messenger.network.dto.AvatarDto
import com.aiwazian.messenger.network.dto.PrivacySettingsResponseDto
import com.aiwazian.messenger.network.dto.UpdateUserRequestDto
import com.aiwazian.messenger.network.dto.UserResponseDto

fun UserResponseDto.toDomain(): User = User(
    id = id,
    firstName = firstName,
    lastName = lastName,
    username = username,
    bio = bio,
    dateOfBirth = dateOfBirth,
    lastSeen = lastSeen,
    avatars = emptyList()
)

fun UserResponseDto.toEntity(): UserEntity = UserEntity(
    id = id,
    firstName = firstName,
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
    invites = invites,
    profilePhoto = profilePhoto,
    deleteAfterDays = deleteAfterDays
)

fun UserEntity.toDomain(avatars: List<Avatar> = emptyList()): User = User(
    id = id,
    firstName = firstName,
    lastName = lastName,
    username = username,
    bio = bio,
    dateOfBirth = dateOfBirth,
    lastSeen = lastSeen,
    avatars = avatars
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

fun AvatarEntity.toDomain(uri: Uri?) = Avatar(
    uri = uri,
    fileId = fileId,
    sortOrder = sortOrder
)

fun AvatarDto.toDomain(uri: Uri? = null) = Avatar(
    uri = uri,
    fileId = fileId,
    sortOrder = sortOrder
)

fun AvatarDto.toEntity(userId: Long) = AvatarEntity(
    fileId = fileId,
    userId = userId,
    sortOrder = sortOrder
)
