/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.api

import com.aiwazian.messenger.network.dto.PrivacySettingsResponseDto
import com.aiwazian.messenger.network.dto.UpdatePrivacySettingsRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

interface PrivacyApi {

    @GET("users/me/privacy")
    suspend fun getPrivacySettings(): Response<PrivacySettingsResponseDto>

    @PATCH("users/me/privacy")
    suspend fun updatePrivacySettings(@Body request: UpdatePrivacySettingsRequestDto): Response<PrivacySettingsResponseDto>
}
