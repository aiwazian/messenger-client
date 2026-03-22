/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SigninResponseDto(
    @SerialName("userId") val userId: String,
    @SerialName("token") val token: String
)

@Serializable
data class SigninRequestDto(
    @SerialName("login") val login: String,
    @SerialName("password") val password: String,
    @SerialName("deviceModel") val deviceModel: String,
    @SerialName("osVersion") val osVersion: String,
    @SerialName("osName") val osName: String
)

@Serializable
data class SignupRequestDto(
    @SerialName("login") val login: String,
    @SerialName("password") val password: String
)

@Serializable
data class LoginAvailableResponseDto(
    @SerialName("available") val available: Boolean
)
