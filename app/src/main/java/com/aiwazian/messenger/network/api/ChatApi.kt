/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.api

import com.aiwazian.messenger.network.dto.ChatResponseDto
import com.aiwazian.messenger.network.dto.DeleteChatRequestDto
import com.aiwazian.messenger.network.dto.InviteLinkInfoDto
import com.aiwazian.messenger.network.dto.PinChatsRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.Path

interface ChatApi {
    
    @GET("chats")
    suspend fun getAllChats(): Response<List<ChatResponseDto>>
    
    @GET("chats/{chatId}")
    suspend fun getChatById(@Path("chatId") chatId: Long): Response<ChatResponseDto>
    
    @POST("chats/{chatId}/archive")
    suspend fun archiveChat(@Path("chatId") chatId: Long): Response<Unit>
    
    @POST("chats/{chatId}/unarchive")
    suspend fun unarchiveChat(@Path("chatId") chatId: Long): Response<Unit>
    
    @HTTP(
        method = "DELETE",
        path = "chats/{chatId}",
        hasBody = true
    )
    suspend fun deleteChat(
        @Path("chatId") chatId: Long,
        @Body request: DeleteChatRequestDto = DeleteChatRequestDto()
    ): Response<Unit>

    @GET("chats/invite-links/{code}/info")
    suspend fun getInviteLinkInfo(@Path("code") code: String): Response<InviteLinkInfoDto>
    
    @GET("chats/join/{code}")
    suspend fun joinViaInviteCode(@Path("code") code: String): Response<Unit>
    
    @HTTP(
        method = "DELETE",
        path = "chats/invite-links/{inviteLinkId}",
        hasBody = false
    )
    suspend fun deleteInviteLink(@Path("inviteLinkId") inviteLinkId: Long): Response<Unit>

    @POST("chats/pin")
    suspend fun pinChats(@Body body: PinChatsRequestDto): Response<Unit>

    @POST("chats/unpin")
    suspend fun unpinChats(@Body body: PinChatsRequestDto): Response<Unit>
}
