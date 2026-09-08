package com.aiwazian.messenger.mappers

import android.net.Uri
import androidx.core.net.toUri
import com.aiwazian.messenger.database.entity.AvatarEntity
import com.aiwazian.messenger.database.entity.AvatarWithFile
import com.aiwazian.messenger.database.entity.UserEntity
import com.aiwazian.messenger.domain.Avatar
import com.aiwazian.messenger.domain.PendingJoinRequest
import com.aiwazian.messenger.domain.PrivacySettings
import com.aiwazian.messenger.domain.User
import com.aiwazian.messenger.network.dto.AvatarDto
import com.aiwazian.messenger.network.dto.PendingJoinRequestDto
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
    avatars = emptyList(),
    profileChannelId = profileChannelId?.toLongOrNull(),
    isBlocked = isBlocked,
    isBlockedByThem = isBlockedByThem,
    canForwardAndCopy = canForwardAndCopy
)

fun UserResponseDto.toEntity(): UserEntity = UserEntity(
    id = id,
    firstName = firstName,
    lastName = lastName,
    username = username,
    bio = bio,
    dateOfBirth = dateOfBirth,
    lastSeen = lastSeen,
    profileChannelId = profileChannelId?.toLongOrNull(),
    isBlocked = isBlocked,
    isBlockedByThem = isBlockedByThem,
    canForwardAndCopy = canForwardAndCopy
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
    forwardedProfile = forwardedProfile,
    forwardAndCopy = forwardAndCopy,
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
    avatars = avatars,
    profileChannelId = profileChannelId,
    isBlocked = isBlocked,
    isBlockedByThem = isBlockedByThem,
    canForwardAndCopy = canForwardAndCopy
)

fun User.toEntity(): UserEntity = UserEntity(
    id = id,
    firstName = firstName,
    lastName = lastName,
    username = username,
    bio = bio,
    dateOfBirth = dateOfBirth,
    lastSeen = lastSeen,
    profileChannelId = profileChannelId,
    isBlocked = isBlocked,
    isBlockedByThem = isBlockedByThem,
    canForwardAndCopy = canForwardAndCopy
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

fun List<AvatarWithFile>.toDomainAvatars(): List<Avatar> =
    sortedByDescending { avatarWithFile -> avatarWithFile.avatar.sortOrder }
        .map { avatarWithFile ->
            val path = avatarWithFile.file?.path
            val uri = if (!path.isNullOrBlank()) path.toUri() else null
            
            avatarWithFile.avatar.toDomain(uri)
        }

fun PendingJoinRequestDto.toDomain() = PendingJoinRequest(
    chatId = chatId.toLong(),
    chatName = chatName,
    createdAt = createdAt.toLong(),
    avatarFileId = avatarFileId
)
