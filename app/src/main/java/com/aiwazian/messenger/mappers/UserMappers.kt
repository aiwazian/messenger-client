/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.network.dto.UserResponseDto
import com.aiwazian.messenger.network.dto.UpdateUserRequestDto
import com.aiwazian.messenger.network.dto.PrivacySettingsResponseDto
import com.aiwazian.messenger.domain.User
import com.aiwazian.messenger.domain.PrivacySettings
import com.aiwazian.messenger.database.entity.UserEntity
import com.aiwazian.messenger.enums.PrivacyLevel

fun UserResponseDto.toDomain(): User = User(
    id = this.id,
    firstName = this.firstName ?: "",
    lastName = this.lastName,
    username = this.username,
    bio = this.bio,
    dateOfBirth = this.dateOfBirth
)

fun UserResponseDto.toEntity(): UserEntity = UserEntity(
    id = this.id,
    firstName = this.firstName ?: "",
    lastName = this.lastName,
    username = this.username,
    bio = this.bio,
    dateOfBirth = this.dateOfBirth
)

fun User.toUpdateRequest(): UpdateUserRequestDto = UpdateUserRequestDto(
    firstName = this.firstName,
    lastName = this.lastName,
    username = this.username,
    bio = this.bio,
    dateOfBirth = this.dateOfBirth
)

fun PrivacySettingsResponseDto.toDomain(): PrivacySettings = PrivacySettings(
    lastSeen = lastSeen,
    messages = messages,
    bio = bio,
    dateOfBirth = dateOfBirth,
    invites = invites
)

fun UserEntity.toDomain(): User = User(
    id = this.id,
    firstName = this.firstName,
    lastName = this.lastName,
    username = this.username,
    bio = this.bio,
    dateOfBirth = this.dateOfBirth
)

fun User.toEntity(): UserEntity {
    return UserEntity(
        id = this.id,
        firstName = this.firstName,
        lastName = this.lastName,
        username = this.username,
        bio = this.bio,
        dateOfBirth = this.dateOfBirth
    )
}

fun UserEntity.toUser(): User {
    return User(
        id = this.id,
        firstName = this.firstName,
        lastName = this.lastName,
        username = this.username,
        bio = this.bio,
        dateOfBirth = this.dateOfBirth
    )
}
