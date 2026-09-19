/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.api

import com.aiwazian.messenger.network.dto.ChatMediaCountsDto
import com.aiwazian.messenger.network.dto.ChatMediaResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatMediaApi {
    
    @GET("chats/{chatId}/media")
    suspend fun getChatMedia(
        @Path("chatId") chatId: Long,
        @Query("cursorId") cursorId: Int? = null,
        @Query("limit") limit: Int? = null
    ): Response<ChatMediaResponseDto>
    
    @GET("chats/{chatId}/files")
    suspend fun getChatFiles(
        @Path("chatId") chatId: Long,
        @Query("cursorId") cursorId: Int? = null,
        @Query("limit") limit: Int? = null
    ): Response<ChatMediaResponseDto>
    
    @GET("chats/{chatId}/music")
    suspend fun getChatMusic(
        @Path("chatId") chatId: Long,
        @Query("cursorId") cursorId: Int? = null,
        @Query("limit") limit: Int? = null
    ): Response<ChatMediaResponseDto>

    @GET("chats/{chatId}/voices")
    suspend fun getChatVoices(
        @Path("chatId") chatId: Long,
        @Query("cursorId") cursorId: Int? = null,
        @Query("limit") limit: Int? = null
    ): Response<ChatMediaResponseDto>
    
    @GET("chats/{chatId}/media-counts")
    suspend fun getChatMediaCounts(
        @Path("chatId") chatId: Long
    ): Response<ChatMediaCountsDto>
}
