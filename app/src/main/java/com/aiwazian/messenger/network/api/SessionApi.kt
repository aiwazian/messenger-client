/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.api

import com.aiwazian.messenger.network.dto.SessionResponseDto
import com.aiwazian.messenger.network.dto.UpdateFcmTokenDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface SessionApi {

    @GET("sessions")
    suspend fun getAllSessions(): Response<List<SessionResponseDto>>
    
    @PATCH("sessions/fcm-token")
    suspend fun updateFcmToken(@Body dto: UpdateFcmTokenDto): Response<Unit>

    @DELETE("sessions/{sessionId}")
    suspend fun deleteSession(@Path("sessionId") sessionId: Int): Response<Unit>

    @DELETE("sessions")
    suspend fun deleteAllSessions(): Response<Unit>
}
