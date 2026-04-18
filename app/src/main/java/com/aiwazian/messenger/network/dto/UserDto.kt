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
    @SerialName("firstName") val firstName: String? = null,
    @SerialName("lastName") val lastName: String? = null,
    @SerialName("username") val username: String? = null,
    @SerialName("bio") val bio: String? = null,
    @SerialName("dateOfBirth") val dateOfBirth: Long? = null,
    @SerialName("lastSeen") val lastSeen: Long? = null
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
data class PrivacySettingsResponseDto(
    @SerialName("lastSeen") val lastSeen: PrivacyLevel,
    @SerialName("messages") val messages: PrivacyLevel,
    @SerialName("bio") val bio: PrivacyLevel,
    @SerialName("dateOfBirth") val dateOfBirth: PrivacyLevel,
    @SerialName("invites") val invites: PrivacyLevel
)

@Serializable
data class UpdatePrivacySettingsRequestDto(
    @SerialName("lastSeen") val lastSeen: PrivacyLevel? = null,
    @SerialName("messages") val messages: PrivacyLevel? = null,
    @SerialName("bio") val bio: PrivacyLevel? = null,
    @SerialName("dateOfBirth") val dateOfBirth: PrivacyLevel? = null,
    @SerialName("invites") val invites: PrivacyLevel? = null
)
