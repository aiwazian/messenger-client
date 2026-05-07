/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.api

import com.aiwazian.messenger.network.dto.LoginAvailableResponseDto
import com.aiwazian.messenger.network.dto.SignInRequestDto
import com.aiwazian.messenger.network.dto.SignInResponseDto
import com.aiwazian.messenger.network.dto.SignUpRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AuthApi {

    @GET("auth/check/{login}")
    suspend fun checkLoginAvailable(@Path("login") login: String): Response<LoginAvailableResponseDto>

    @POST("auth/signin")
    suspend fun signIn(@Body request: SignInRequestDto): Response<SignInResponseDto>

    @POST("auth/signup")
    suspend fun signUp(@Body request: SignUpRequestDto): Response<SignInResponseDto>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>
}
