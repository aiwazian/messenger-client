/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.api

import com.aiwazian.messenger.network.dto.FileInitResponseDto
import com.aiwazian.messenger.network.dto.InitWallpaperUploadRequestDto
import com.aiwazian.messenger.network.dto.SetChatWallpaperRequestDto
import com.aiwazian.messenger.network.dto.SetWallpaperRequestDto
import com.aiwazian.messenger.network.dto.WallpaperDto
import com.aiwazian.messenger.network.dto.WallpaperResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface WallpaperApi {

    @POST("users/me/wallpaper/init")
    suspend fun initUpload(@Body request: InitWallpaperUploadRequestDto): Response<FileInitResponseDto>

    @POST("users/me/wallpaper/confirm/{fileId}")
    suspend fun confirmUpload(@Path("fileId") fileId: String): Response<Unit>

    @GET("users/me/wallpaper")
    suspend fun getMyWallpaper(): Response<WallpaperResponseDto>

    @PUT("users/me/wallpaper")
    suspend fun setMyWallpaper(@Body request: SetWallpaperRequestDto): Response<WallpaperDto>

    @DELETE("users/me/wallpaper")
    suspend fun resetMyWallpaper(): Response<Unit>

    @GET("chats/{chatId}/wallpaper")
    suspend fun getChatWallpaper(@Path("chatId") chatId: Long): Response<WallpaperResponseDto>

    @PUT("chats/{chatId}/wallpaper")
    suspend fun setChatWallpaper(
        @Path("chatId") chatId: Long,
        @Body request: SetChatWallpaperRequestDto
    ): Response<WallpaperDto>

    @DELETE("chats/{chatId}/wallpaper")
    suspend fun resetChatWallpaper(
        @Path("chatId") chatId: Long,
        @Query("scope") scope: String
    ): Response<Unit>

    @PUT("channels/{channelId}/wallpaper")
    suspend fun setChannelWallpaper(
        @Path("channelId") channelId: Long,
        @Body request: SetWallpaperRequestDto
    ): Response<WallpaperDto>

    @DELETE("channels/{channelId}/wallpaper")
    suspend fun resetChannelWallpaper(@Path("channelId") channelId: Long): Response<Unit>

    @PUT("groups/{groupId}/wallpaper")
    suspend fun setGroupWallpaper(
        @Path("groupId") groupId: Long,
        @Body request: SetWallpaperRequestDto
    ): Response<WallpaperDto>

    @DELETE("groups/{groupId}/wallpaper")
    suspend fun resetGroupWallpaper(@Path("groupId") groupId: Long): Response<Unit>
}
