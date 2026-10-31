package com.aiwazian.messenger.network.api

import com.aiwazian.messenger.network.dto.ChatReadStateDto
import com.aiwazian.messenger.network.dto.ClearHistoryRequestDto
import com.aiwazian.messenger.network.dto.ContactMessageRequestDto
import com.aiwazian.messenger.network.dto.DeleteMessageRequestDto
import com.aiwazian.messenger.network.dto.EditMessageRequestDto
import com.aiwazian.messenger.network.dto.FileConfirmRequestDto
import com.aiwazian.messenger.network.dto.FileDownloadResponseDto
import com.aiwazian.messenger.network.dto.FileInitRequestDto
import com.aiwazian.messenger.network.dto.FileInitResponseDto
import com.aiwazian.messenger.network.dto.ForwardMessageRequestDto
import com.aiwazian.messenger.network.dto.MarkReadRequestDto
import com.aiwazian.messenger.network.dto.MessageDto
import com.aiwazian.messenger.network.dto.MessagePinResponseDto
import com.aiwazian.messenger.network.dto.MessageSearchResponseDto
import com.aiwazian.messenger.network.dto.MessagesWindowDto
import com.aiwazian.messenger.network.dto.PinMessageRequestDto
import com.aiwazian.messenger.network.dto.StickerMessageRequestDto
import com.aiwazian.messenger.network.dto.TextMessageRequestDto
import com.aiwazian.messenger.network.dto.UpdateScheduledMessageRequestDto
import com.aiwazian.messenger.network.dto.ViewOnceFileIdsDto
import com.aiwazian.messenger.network.dto.ViewOnceViewedDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
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

    @POST("chats/{chatId}/messages/contacts")
    suspend fun sendContactMessage(
        @Path("chatId") chatId: Long,
        @Body request: ContactMessageRequestDto,
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

    @GET("chats/{chatId}/messages/view-once")
    suspend fun getActiveViewOnceFileIds(
        @Path("chatId") chatId: Long
    ): Response<ViewOnceFileIdsDto>

    @POST("chats/{chatId}/messages/{messageId}/attachments/{fileId}/view")
    suspend fun markViewOnceViewed(
        @Path("chatId") chatId: Long,
        @Path("messageId") messageId: Long,
        @Path("fileId") fileId: String
    ): Response<ViewOnceViewedDto>
    
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

    @GET("chats/{chatId}/messages/pinned")
    suspend fun getPinnedMessages(
        @Path("chatId") chatId: Long
    ): Response<List<MessagePinResponseDto>>

    @POST("chats/{chatId}/messages/{messageId}/pin")
    suspend fun pinMessage(
        @Path("chatId") chatId: Long,
        @Path("messageId") messageId: Long,
        @Body request: PinMessageRequestDto,
        @Header("x-socket-id") socketId: String
    ): Response<MessagePinResponseDto>

    @DELETE("chats/{chatId}/messages/{messageId}/pin")
    suspend fun unpinMessage(
        @Path("chatId") chatId: Long,
        @Path("messageId") messageId: Long,
        @Header("x-socket-id") socketId: String
    ): Response<Unit>

    @GET("chats/{chatId}/messages/scheduled")
    suspend fun getScheduledMessages(
        @Path("chatId") chatId: Long
    ): Response<List<MessageDto>>

    @retrofit2.http.PATCH("chats/{chatId}/messages/{messageId}/schedule")
    suspend fun updateScheduledMessage(
        @Path("chatId") chatId: Long,
        @Path("messageId") messageId: Long,
        @Body request: UpdateScheduledMessageRequestDto,
        @Header("x-socket-id") socketId: String
    ): Response<MessageDto>

    @DELETE("chats/{chatId}/messages/{messageId}/schedule")
    suspend fun deleteScheduledMessage(
        @Path("chatId") chatId: Long,
        @Path("messageId") messageId: Long,
        @Header("x-socket-id") socketId: String
    ): Response<Unit>

    @POST("chats/{chatId}/messages/{messageId}/schedule/send-now")
    suspend fun sendScheduledMessageNow(
        @Path("chatId") chatId: Long,
        @Path("messageId") messageId: Long
    ): Response<MessageDto>
}
