/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.api

import com.aiwazian.messenger.network.dto.ChatResponseDto
import com.aiwazian.messenger.network.dto.CreateInviteLinkRequestDto
import com.aiwazian.messenger.network.dto.InviteLinkInfoDto
import com.aiwazian.messenger.network.dto.InviteLinkResponseDto
import com.aiwazian.messenger.network.dto.MessageResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ChatApi {

    @GET("chats")
    suspend fun getAllChats(): Response<List<ChatResponseDto>>

    @GET("chats/{chatId}")
    suspend fun getChatById(@Path("chatId") chatId: Long): Response<ChatResponseDto>

    @GET("chats/{chatId}/last-message")
    suspend fun getLastMessage(@Path("chatId") chatId: Long): Response<MessageResponseDto>

    @POST("chats/{chatId}/archive")
    suspend fun archiveChat(@Path("chatId") chatId: Long): Response<Unit>

    @POST("chats/{chatId}/unarchive")
    suspend fun unarchiveChat(@Path("chatId") chatId: Long): Response<Unit>

    @DELETE("chats/{chatId}")
    suspend fun deleteChat(@Path("chatId") chatId: Long): Response<Unit>

    @POST("chats/invite-links")
    suspend fun createInviteLink(@Body request: CreateInviteLinkRequestDto): Response<InviteLinkResponseDto>

    @GET("chats/invite-links/{code}/info")
    suspend fun getInviteLinkInfo(@Path("code") code: String): Response<InviteLinkInfoDto>

    @GET("chats/join/{code}")
    suspend fun joinViaInviteCode(@Path("code") code: String): Response<Unit>

    @DELETE("chats/invite-links/{inviteLinkId}")
    suspend fun deleteInviteLink(@Path("inviteLinkId") inviteLinkId: Long): Response<Unit>
}
