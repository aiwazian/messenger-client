/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.api

import com.aiwazian.messenger.network.dto.ChatFolderDto
import com.aiwazian.messenger.network.dto.CreateChatFolderRequestDto
import com.aiwazian.messenger.network.dto.PinFolderChatsRequestDto
import com.aiwazian.messenger.network.dto.ReorderChatFoldersRequestDto
import com.aiwazian.messenger.network.dto.UpdateChatFolderRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface ChatFolderApi {
    
    @GET("chat-folders")
    suspend fun getFolders(): Response<List<ChatFolderDto>>
    
    @POST("chat-folders")
    suspend fun createFolder(
        @Body request: CreateChatFolderRequestDto,
        @Header("x-socket-id") socketId: String? = null
    ): Response<ChatFolderDto>
    
    @PATCH("chat-folders/{folderId}")
    suspend fun updateFolder(
        @Path("folderId") folderId: Int,
        @Body request: UpdateChatFolderRequestDto
    ): Response<ChatFolderDto>
    
    @DELETE("chat-folders/{folderId}")
    suspend fun deleteFolder(
        @Path("folderId") folderId: Int,
        @Header("x-socket-id") socketId: String? = null
    ): Response<Unit>
    
    @POST("chat-folders/{folderId}/pin")
    suspend fun pinChats(
        @Path("folderId") folderId: Int,
        @Body request: PinFolderChatsRequestDto
    ): Response<Unit>
    
    @POST("chat-folders/{folderId}/unpin")
    suspend fun unpinChats(
        @Path("folderId") folderId: Int,
        @Body request: PinFolderChatsRequestDto
    ): Response<Unit>
    
    @POST("chat-folders/reorder")
    suspend fun reorderFolders(
        @Body request: ReorderChatFoldersRequestDto
    ): Response<List<ChatFolderDto>>
}
