package com.aiwazian.messenger.network.api

import com.aiwazian.messenger.network.dto.ChatReadStateDto
import com.aiwazian.messenger.network.dto.ClearHistoryRequestDto
import com.aiwazian.messenger.network.dto.DeleteMessageRequestDto
import com.aiwazian.messenger.network.dto.EditMessageRequestDto
import com.aiwazian.messenger.network.dto.FileConfirmRequestDto
import com.aiwazian.messenger.network.dto.FileDownloadResponseDto
import com.aiwazian.messenger.network.dto.FileInitRequestDto
import com.aiwazian.messenger.network.dto.FileInitResponseDto
import com.aiwazian.messenger.network.dto.ForwardMessageRequestDto
import com.aiwazian.messenger.network.dto.MarkReadRequestDto
import com.aiwazian.messenger.network.dto.MessageDto
import com.aiwazian.messenger.network.dto.MessageSearchResponseDto
import com.aiwazian.messenger.network.dto.MessagesWindowDto
import com.aiwazian.messenger.network.dto.StickerMessageRequestDto
import com.aiwazian.messenger.network.dto.TextMessageRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MessageApi {
    
    @GET("chats/{chatId}/messages/window")
    suspend fun getMessagesWindow(
        @Path("chatId") chatId: Long,
        @Query("anchorId") anchorId: Long? = null,
        @Query("beforeId") beforeId: Long? = null,
        @Query("afterId") afterId: Long? = null,
        @Query("anchor") anchor: String? = null,
        @Query("limit") limit: Int? = null
    ): Response<MessagesWindowDto>
    
    @GET("chats/{chatId}/messages/search")
    suspend fun searchMessages(
        @Path("chatId") chatId: Long,
        @Query("q") query: String,
        @Query("cursorId") cursorId: Long? = null,
        @Query("limit") limit: Int? = null
    ): Response<MessageSearchResponseDto>
    
    @GET("chats/{chatId}/messages")
    suspend fun getMessages(
        @Path("chatId") chatId: Long,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): Response<List<MessageDto>>
    
    @POST("chats/{chatId}/messages")
    suspend fun sendTextMessage(
        @Path("chatId") chatId: Long,
        @Body request: TextMessageRequestDto,
        @Header("x-socket-id") socketId: String
    ): Response<MessageDto>
    
    @POST("chats/{chatId}/messages/stickers")
    suspend fun sendStickerMessage(
        @Path("chatId") chatId: Long,
        @Body request: StickerMessageRequestDto,
        @Header("x-socket-id") socketId: String
    ): Response<MessageDto>
    
    @POST("chats/{chatId}/messages/files/init")
    suspend fun initFileUpload(
        @Path("chatId") chatId: Long,
        @Body request: FileInitRequestDto
    ): Response<FileInitResponseDto>
    
    @POST("chats/{chatId}/messages/files/confirm")
    suspend fun confirmFileUpload(
        @Path("chatId") chatId: Long,
        @Body request: FileConfirmRequestDto,
        @Header("x-socket-id") socketId: String
    ): Response<MessageDto>
    
    @GET("chats/{chatId}/messages/{messageId}/files/{fileId}/download")
    suspend fun getFileDownloadUrl(
        @Path("chatId") chatId: Long,
        @Path("messageId") messageId: Long,
        @Path("fileId") fileId: String
    ): Response<FileDownloadResponseDto>
    
    @POST("chats/{chatId}/messages/{messageId}/forward")
    suspend fun forwardMessage(
        @Path("chatId") chatId: Long,
        @Path("messageId") messageId: Long,
        @Body request: ForwardMessageRequestDto,
        @Header("x-socket-id") socketId: String
    ): Response<List<MessageDto>>
    
    @POST("chats/{chatId}/messages/{messageId}/read")
    suspend fun markRead(
        @Path("chatId") chatId: Long,
        @Path("messageId") messageId: Long
    ): Response<ChatReadStateDto>
    
    @POST("chats/{chatId}/messages/read")
    suspend fun markAllRead(
        @Path("chatId") chatId: Long,
        @Body request: MarkReadRequestDto = MarkReadRequestDto()
    ): Response<ChatReadStateDto>
    
    @HTTP(
        method = "DELETE",
        path = "chats/{chatId}/messages/{messageId}",
        hasBody = true
    )
    suspend fun deleteMessage(
        @Path("chatId") chatId: Long,
        @Path("messageId") messageId: Long,
        @Body request: DeleteMessageRequestDto = DeleteMessageRequestDto()
    ): Response<Unit>
    
    @HTTP(
        method = "DELETE",
        path = "chats/{chatId}/messages",
        hasBody = true
    )
    suspend fun clearHistory(
        @Path("chatId") chatId: Long,
        @Body request: ClearHistoryRequestDto = ClearHistoryRequestDto()
    ): Response<Unit>

    @retrofit2.http.PATCH("chats/{chatId}/messages/{messageId}")
    suspend fun editTextMessage(
        @Path("chatId") chatId: Long,
        @Path("messageId") messageId: Long,
        @Body request: EditMessageRequestDto,
        @Header("x-socket-id") socketId: String
    ): Response<MessageDto>
}
