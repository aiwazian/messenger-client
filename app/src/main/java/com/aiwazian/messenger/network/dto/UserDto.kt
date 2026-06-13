/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dto

import com.aiwazian.messenger.enums.PrivacyLevel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserResponseDto(
    @SerialName("id") val id: Long,
    @SerialName("firstName") val firstName: String,
    @SerialName("lastName") val lastName: String? = null,
    @SerialName("username") val username: String? = null,
    @SerialName("bio") val bio: String? = null,
    @SerialName("dateOfBirth") val dateOfBirth: Long? = null,
    @SerialName("lastSeen") val lastSeen: Long? = null,
    @SerialName("avatars") val avatars: List<AvatarDto> = emptyList()
)

@Serializable
data class AvatarDto(
    @SerialName("fileId") val fileId: String,
    @SerialName("sortOrder") val sortOrder: Int = 0
)

@Serializable
data class UpdateUserRequestDto(
    @SerialName("firstName") val firstName: String,
    @SerialName("lastName") val lastName: String? = null,
    @SerialName("username") val username: String? = null,
    @SerialName("bio") val bio: String? = null,
    @SerialName("dateOfBirth") val dateOfBirth: Long? = null
)

@Serializable
data class ChangePasswordRequestDto(
    @SerialName("password") val password: String
)

@Serializable
data class ChangeLoginRequestDto(
    @SerialName("login") val login: String
)

@Serializable
data class PrivacySettingsResponseDto(
    @SerialName("lastSeen") val lastSeen: PrivacyLevel = PrivacyLevel.EVERYBODY,
    @SerialName("messages") val messages: PrivacyLevel = PrivacyLevel.EVERYBODY,
    @SerialName("bio") val bio: PrivacyLevel = PrivacyLevel.EVERYBODY,
    @SerialName("dateOfBirth") val dateOfBirth: PrivacyLevel = PrivacyLevel.EVERYBODY,
    @SerialName("invites") val invites: PrivacyLevel = PrivacyLevel.EVERYBODY,
    @SerialName("profilePhoto") val profilePhoto: PrivacyLevel = PrivacyLevel.EVERYBODY,
    @SerialName("deleteAfterDays") val deleteAfterDays: Int = 365
)

@Serializable
data class UpdatePrivacySettingsRequestDto(
    @SerialName("lastSeen") val lastSeen: PrivacyLevel? = null,
    @SerialName("messages") val messages: PrivacyLevel? = null,
    @SerialName("bio") val bio: PrivacyLevel? = null,
    @SerialName("dateOfBirth") val dateOfBirth: PrivacyLevel? = null,
    @SerialName("invites") val invites: PrivacyLevel? = null,
    @SerialName("profilePhoto") val profilePhoto: PrivacyLevel? = null,
    @SerialName("deleteAfterDays") val deleteAfterDays: Int? = null
)
