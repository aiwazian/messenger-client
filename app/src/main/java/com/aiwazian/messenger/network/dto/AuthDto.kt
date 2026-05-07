/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SignInResponseDto(
    @SerialName("userId") val userId: Long,
    @SerialName("token") val token: String,
    @SerialName("createdAt") val createdAt: Long
)

@Serializable
data class SignInRequestDto(
    @SerialName("login") val login: String,
    @SerialName("password") val password: String,
    @SerialName("deviceModel") val deviceModel: String,
    @SerialName("osVersion") val osVersion: String,
    @SerialName("osName") val osName: String
)

@Serializable
data class SignUpRequestDto(
    @SerialName("firstName") val firstName: String,
    @SerialName("login") val login: String,
    @SerialName("password") val password: String,
    @SerialName("deviceModel") val deviceModel: String,
    @SerialName("osVersion") val osVersion: String,
    @SerialName("osName") val osName: String
)

@Serializable
data class LoginAvailableResponseDto(
    @SerialName("available") val available: Boolean
)
