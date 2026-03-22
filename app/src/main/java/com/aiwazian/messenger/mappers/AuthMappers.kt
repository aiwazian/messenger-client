/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.domain.SignInRequest
import com.aiwazian.messenger.domain.SignInResponse
import com.aiwazian.messenger.domain.SignUpRequest
import com.aiwazian.messenger.network.dto.SigninResponseDto
import com.aiwazian.messenger.network.dto.SigninRequestDto
import com.aiwazian.messenger.network.dto.SignupRequestDto

fun SigninResponseDto.toAuthResult(): SignInResponse {
    return SignInResponse(
        userId = this.userId.toLong(),
        token = this.token
    )
}

fun SignInRequest.toDto(): SigninRequestDto {
    return SigninRequestDto(
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