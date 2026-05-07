/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.domain.SignInRequest
import com.aiwazian.messenger.domain.SignInResponse
import com.aiwazian.messenger.domain.SignUpRequest
import com.aiwazian.messenger.network.dto.SignInRequestDto
import com.aiwazian.messenger.network.dto.SignInResponseDto
import com.aiwazian.messenger.network.dto.SignUpRequestDto

fun SignInRequest.toDto() = SignInRequestDto(
    login = login,
    password = password,
    deviceModel = deviceModel,
    osVersion = osVersion,
    osName = osName
)

fun SignUpRequest.toDto() = SignUpRequestDto(
    firstName = firstName,
    login = login,
    password = password,
    deviceModel = deviceModel,
    osVersion = osVersion,
    osName = osName
)

fun SignInResponseDto.toDomain() = SignInResponse(
    userId = userId,
    token = token,
    createdAt = createdAt
)
