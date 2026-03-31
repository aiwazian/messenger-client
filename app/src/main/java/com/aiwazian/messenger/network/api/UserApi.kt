/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.api

import com.aiwazian.messenger.network.dto.ChangePasswordRequestDto
import com.aiwazian.messenger.network.dto.UpdateUserRequestDto
import com.aiwazian.messenger.network.dto.UserResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface UserApi {

    @GET("users/me")
    suspend fun getMe(): Response<UserResponseDto>

    @PATCH("users/me")
    suspend fun updateMe(@Body request: UpdateUserRequestDto): Response<UserResponseDto>

    @PATCH("users/me/password")
    suspend fun changePassword(@Body request: ChangePasswordRequestDto): Response<Unit>

    @GET("users/{userId}")
    suspend fun getUserById(@Path("userId") userId: Long): Response<UserResponseDto>

    @DELETE("users/me")
    suspend fun deleteMe(): Response<Unit>
}
