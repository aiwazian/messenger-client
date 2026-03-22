/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.api

import com.aiwazian.messenger.network.dto.SessionResponseDto
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path

interface SessionApi {

    @GET("sessions")
    suspend fun getAllSessions(): Response<List<SessionResponseDto>>

    @DELETE("sessions/{sessionId}")
    suspend fun deleteSession(@Path("sessionId") sessionId: Int): Response<Unit>

    @DELETE("sessions")
    suspend fun deleteAllSessions(): Response<Unit>
}
