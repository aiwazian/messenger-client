/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.api

import com.aiwazian.messenger.network.dto.ChangeLoginRequestDto
import com.aiwazian.messenger.network.dto.ChangePasswordRequestDto
import com.aiwazian.messenger.network.dto.FileDownloadResponseDto
import com.aiwazian.messenger.network.dto.FileInitRequestDto
import com.aiwazian.messenger.network.dto.FileInitResponseDto
import com.aiwazian.messenger.network.dto.OwnedChannelDto
import com.aiwazian.messenger.network.dto.SetProfileChannelRequestDto
import com.aiwazian.messenger.network.dto.UpdateUserRequestDto
import com.aiwazian.messenger.network.dto.UserResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface UserApi {
    
    @GET("users/me")
    suspend fun getMe(): Response<UserResponseDto>
    
    @PATCH("users/me")
    suspend fun updateMe(@Body request: UpdateUserRequestDto): Response<UserResponseDto>
    
    @PATCH("users/me/password")
    suspend fun changePassword(@Body request: ChangePasswordRequestDto): Response<Unit>
    
    @PATCH("users/me/login")
    suspend fun changeLogin(@Body request: ChangeLoginRequestDto): Response<Unit>
    
    @GET("users/{userId}")
    suspend fun getUserById(@Path("userId") userId: Long): Response<UserResponseDto>
    
    @DELETE("users/me")
    suspend fun deleteMe(): Response<Unit>
    
    @POST("users/me/avatar/init")
    suspend fun initUploadAvatar(@Body body: FileInitRequestDto): Response<FileInitResponseDto>
    
    @POST("users/me/avatar/confirm/{fileId}")
    suspend fun confirmUploadAvatar(@Path("fileId") fileId: String): Response<Unit>
    
    @DELETE("users/me/avatars/{fileId}")
    suspend fun deleteAvatar(@Path("fileId") fileId: String): Response<Unit>
    
    @GET("users/avatars/{fileId}")
    suspend fun getAvatarDownloadUrl(@Path("fileId") fileId: String): Response<FileDownloadResponseDto>
    
    @PATCH("users/me/profile-channel")
    suspend fun setProfileChannel(@Body request: SetProfileChannelRequestDto): Response<Unit>
    
    @DELETE("users/me/profile-channel")
    suspend fun removeProfileChannel(): Response<Unit>
    
    @GET("users/me/owned-channels")
    suspend fun getOwnedChannels(): Response<List<OwnedChannelDto>>
    
    @GET("users/me/blocked")
    suspend fun getBlockedUsers(): Response<List<UserResponseDto>>
    
    @POST("users/{userId}/block")
    suspend fun blockUser(@Path("userId") userId: Long): Response<Unit>
    
    @DELETE("users/{userId}/block")
    suspend fun unblockUser(@Path("userId") userId: Long): Response<Unit>
    
    @GET("users/me/join-requests")
    suspend fun getPendingJoinRequests(): Response<List<com.aiwazian.messenger.network.dto.PendingJoinRequestDto>>
    
    @DELETE("users/me/join-requests/{chatId}")
    suspend fun cancelJoinRequest(@Path("chatId") chatId: Long): Response<Unit>
    
    @GET("users/me/email")
    suspend fun getEmail(): Response<com.aiwazian.messenger.network.dto.EmailResponseDto>
    
    @PATCH("users/me/email")
    suspend fun setEmail(@Body request: com.aiwazian.messenger.network.dto.SetEmailRequestDto): Response<Unit>
    
    @POST("users/me/email/verify")
    suspend fun verifyEmail(@Body request: com.aiwazian.messenger.network.dto.VerifyEmailRequestDto): Response<com.aiwazian.messenger.network.dto.EmailResponseDto>
    
    @DELETE("users/me/email")
    suspend fun disableEmail(): Response<Unit>
}
