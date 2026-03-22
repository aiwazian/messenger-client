/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.api

import com.aiwazian.messenger.network.dto.SigninResponseDto
import com.aiwazian.messenger.network.dto.LoginAvailableResponseDto
import com.aiwazian.messenger.network.dto.SigninRequestDto
import com.aiwazian.messenger.network.dto.SignupRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AuthApi {

    @GET("auth/check/{login}")
    suspend fun checkLoginAvailable(@Path("login") login: String): Response<LoginAvailableResponseDto>

    @POST("auth/signin")
    suspend fun signIn(@Body request: SigninRequestDto): Response<SigninResponseDto>

    @POST("auth/signup")
    suspend fun signUp(@Body request: SignupRequestDto): Response<Unit>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>
}
