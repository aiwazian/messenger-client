/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.api

import com.aiwazian.messenger.network.dto.NotificationSettingsResponseDto
import com.aiwazian.messenger.network.dto.UpdateNotificationSettingsRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

interface NotificationSettingsApi {

    @GET("users/me/notifications")
    suspend fun getNotificationSettings(): Response<NotificationSettingsResponseDto>

    @PATCH("users/me/notifications")
    suspend fun updateNotificationSettings(@Body request: UpdateNotificationSettingsRequestDto): Response<NotificationSettingsResponseDto>
}
