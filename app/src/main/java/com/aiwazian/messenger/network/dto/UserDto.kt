/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserResponseDto(
    @SerialName("id") val id: Long,
    @SerialName("firstName") val firstName: String? = null,
    @SerialName("lastName") val lastName: String? = null,
    @SerialName("username") val username: String? = null,
    @SerialName("bio") val bio: String? = null,
    @SerialName("dateOfBirth") val dateOfBirth: Long? = null
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
    @SerialName("lastSeen") val lastSeen: Int,
    @SerialName("messages") val messages: Int,
    @SerialName("bio") val bio: Int,
    @SerialName("dateOfBirth") val dateOfBirth: Int,
    @SerialName("invites") val invites: Int
)

@Serializable
data class UpdatePrivacySettingsRequestDto(
    @SerialName("lastSeen") val lastSeen: Int? = null,
    @SerialName("messages") val messages: Int? = null,
    @SerialName("bio") val bio: Int? = null,
    @SerialName("dateOfBirth") val dateOfBirth: Int? = null,
    @SerialName("invites") val invites: Int? = null
)
