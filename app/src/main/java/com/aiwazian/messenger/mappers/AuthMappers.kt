/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.domain.SignInRequest
import com.aiwazian.messenger.domain.SignInResponse
import com.aiwazian.messenger.domain.SignUpRequest
import com.aiwazian.messenger.network.dto.SignInResponseDto
import com.aiwazian.messenger.network.dto.SignInRequestDto
import com.aiwazian.messenger.network.dto.SignupRequestDto

fun SignInResponseDto.toAuthResult(): SignInResponse {
    return SignInResponse(
        userId = this.userId.toLong(),
        token = this.token
    )
}

fun SignInRequest.toDto(): SignInRequestDto {
    return SignInRequestDto(
        login = this.login,
        password = this.password,
        deviceModel = this.deviceModel,
        osVersion = this.osVersion,
        osName = this.osName
    )
}

fun SignUpRequest.toDto(): SignupRequestDto {
    return SignupRequestDto(
        login = login,
        password = password
    )
}