/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.api

import com.aiwazian.messenger.network.dto.ChannelResponseDto
import com.aiwazian.messenger.network.dto.CreateChannelRequestDto
import com.aiwazian.messenger.network.dto.CreateInviteLinkRequestDto
import com.aiwazian.messenger.network.dto.FileDownloadResponseDto
import com.aiwazian.messenger.network.dto.FileInitRequestDto
import com.aiwazian.messenger.network.dto.FileInitResponseDto
import com.aiwazian.messenger.network.dto.InviteLinkResponseDto
import com.aiwazian.messenger.network.dto.UpdateChannelRequestDto
import com.aiwazian.messenger.network.dto.UserResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChannelApi {

    @POST("channels")
    suspend fun createChannel(@Body request: CreateChannelRequestDto): Response<ChannelResponseDto>

    @GET("channels/{channelId}")
    suspend fun getChannelById(@Path("channelId") channelId: Long): Response<ChannelResponseDto>

    @GET("channels/{channelId}/subscribers")
    suspend fun getChannelSubscribers(
        @Path("channelId") channelId: Long,
        @Query("skip") skip: Int = 0,
        @Query("take") take: Int = 100,
        @Query("search") search: String? = null
    ): Response<List<UserResponseDto>>

    @PATCH("channels/{channelId}")
    suspend fun updateChannel(
        @Path("channelId") channelId: Long,
        @Body request: UpdateChannelRequestDto
    ): Response<ChannelResponseDto>

    @DELETE("channels/{channelId}")
    suspend fun deleteChannel(@Path("channelId") channelId: Long): Response<Unit>

    @POST("channels/{channelId}/join")
    suspend fun joinChannel(@Path("channelId") channelId: Long): Response<Unit>

    @DELETE("channels/{channelId}/leave")
    suspend fun leaveChannel(@Path("channelId") channelId: Long): Response<Unit>

    @POST("channels/{channelId}/kick/{userId}")
    suspend fun kickUser(
        @Path("channelId") channelId: Long,
        @Path("userId") userId: Long
    ): Response<Unit>

    @POST("channels/{channelId}/ban/{userId}")
    suspend fun banUser(
        @Path("channelId") channelId: Long,
        @Path("userId") userId: Long
    ): Response<Unit>

    @GET("channels/{channelId}/invite-links")
    suspend fun getInviteLinks(@Path("channelId") channelId: Long): Response<List<InviteLinkResponseDto>>

    @POST("channels/{channelId}/invite-links")
    suspend fun createInviteLink(
        @Path("channelId") channelId: Long,
        @Body request: CreateInviteLinkRequestDto
    ): Response<InviteLinkResponseDto>

    @DELETE("channels/{channelId}/invite-links/{inviteLinkId}")
    suspend fun deleteInviteLink(
        @Path("channelId") channelId: Long,
        @Path("inviteLinkId") inviteLinkId: Long
    ): Response<Unit>

    @GET("channels/{channelId}/banned-users")
    suspend fun getBannedUsers(
        @Path("channelId") channelId: Long,
        @Query("skip") skip: Int = 0,
        @Query("take") take: Int = 100,
        @Query("search") search: String? = null
    ): Response<List<UserResponseDto>>

    @POST("channels/{channelId}/unban/{userId}")
    suspend fun unbanUser(
        @Path("channelId") channelId: Long,
        @Path("userId") userId: Long
    ): Response<Unit>
    
    @GET("channels/{channelId}/join-requests")
    suspend fun getJoinRequests(
        @Path("channelId") channelId: Long,
        @Query("skip") skip: Int = 0,
        @Query("take") take: Int = 100,
        @Query("search") search: String? = null
    ): Response<List<UserResponseDto>>
    
    @POST("channels/{channelId}/join-requests/{userId}/accept")
    suspend fun acceptJoinRequest(
        @Path("channelId") channelId: Long,
        @Path("userId") userId: Long
    ): Response<Unit>
    
    @POST("channels/{channelId}/join-requests/{userId}/reject")
    suspend fun rejectJoinRequest(
        @Path("channelId") channelId: Long,
        @Path("userId") userId: Long
    ): Response<Unit>
    
    @POST("channels/{channelId}/avatar/init")
    suspend fun initUploadAvatar(
        @Path("channelId") channelId: Long,
        @Body request: FileInitRequestDto
    ): Response<FileInitResponseDto>
    
    @POST("channels/{channelId}/avatar/confirm/{fileId}")
    suspend fun confirmUploadAvatar(
        @Path("channelId") channelId: Long,
        @Path("fileId") fileId: String
    ): Response<Unit>
    
    @DELETE("channels/{channelId}/avatars/{fileId}")
    suspend fun deleteAvatar(
        @Path("channelId") channelId: Long,
        @Path("fileId") fileId: String
    ): Response<Unit>
    
    @GET("channels/avatars/{fileId}")
    suspend fun getAvatarDownloadUrl(@Path("fileId") fileId: String): Response<FileDownloadResponseDto>
}
