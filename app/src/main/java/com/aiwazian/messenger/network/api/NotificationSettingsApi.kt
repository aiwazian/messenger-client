/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.api

import com.aiwazian.messenger.network.dto.ChatNotificationSettingDto
import com.aiwazian.messenger.network.dto.NotificationSettingsResponseDto
import com.aiwazian.messenger.network.dto.UpdateChatNotificationSettingRequestDto
import com.aiwazian.messenger.network.dto.UpdateNotificationSettingsRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationSettingsApi {

    @GET("users/me/notifications")
    suspend fun getNotificationSettings(): Response<NotificationSettingsResponseDto>

    @PATCH("users/me/notifications")
    suspend fun updateNotificationSettings(@Body request: UpdateNotificationSettingsRequestDto): Response<NotificationSettingsResponseDto>

    @GET("users/me/notifications/chats")
    suspend fun getChatNotificationSettings(): Response<List<ChatNotificationSettingDto>>

    @PUT("users/me/notifications/chats/{chatId}")
    suspend fun setChatNotificationSetting(
        @Path("chatId") chatId: Long,
        @Body request: UpdateChatNotificationSettingRequestDto
    ): Response<ChatNotificationSettingDto>

    @DELETE("users/me/notifications/chats/{chatId}")
    suspend fun deleteChatNotificationSetting(@Path("chatId") chatId: Long): Response<Unit>

    @DELETE("users/me/notifications/chats")
    suspend fun deleteAllChatNotificationSettings(
        @Query("category") category: String? = null
    ): Response<Unit>
}
