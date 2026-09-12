package com.aiwazian.messenger.network.api

import com.aiwazian.messenger.network.dto.CreateEmojiPackRequestDto
import com.aiwazian.messenger.network.dto.CustomEmojiItemDto
import com.aiwazian.messenger.network.dto.EmojiFileDto
import com.aiwazian.messenger.network.dto.EmojiPackDto
import com.aiwazian.messenger.network.dto.EmojiPackIdDto
import com.aiwazian.messenger.network.dto.EmojiPackUsernameAvailabilityDto
import com.aiwazian.messenger.network.dto.EmojiUploadInitRequestDto
import com.aiwazian.messenger.network.dto.FileInitResponseDto
import com.aiwazian.messenger.network.dto.UpdateEmojiPackRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface EmojiApi {
    
    @GET("emoji/packs/created")
    suspend fun getCreatedPacks(): Response<List<EmojiPackDto>>
    
    @GET("emoji/packs/added")
    suspend fun getAddedPacks(
        @Query("includeEmojis") includeEmojis: Boolean? = null
    ): Response<List<EmojiPackDto>>
    
    @GET("emoji/packs/username-available")
    suspend fun checkUsername(
        @Query("username") username: String,
        @Query("packId") packId: String? = null
    ): Response<EmojiPackUsernameAvailabilityDto>
    
    @GET("emoji/packs/by-username/{username}")
    suspend fun getPackByUsername(
        @Path("username") username: String
    ): Response<EmojiPackDto>
    
    @GET("emoji/packs/{packId}")
    suspend fun getPack(
        @Path("packId") packId: String
    ): Response<EmojiPackDto>
    
    @GET("emoji/items")
    suspend fun getEmojiItems(
        @Query("ids") ids: String
    ): Response<List<CustomEmojiItemDto>>
    
    @POST("emoji/packs/reserve")
    suspend fun reservePackId(): Response<EmojiPackIdDto>
    
    @POST("emoji/packs")
    suspend fun createPack(
        @Body request: CreateEmojiPackRequestDto
    ): Response<EmojiPackDto>
    
    @PATCH("emoji/packs/{packId}")
    suspend fun updatePack(
        @Path("packId") packId: String,
        @Body request: UpdateEmojiPackRequestDto
    ): Response<EmojiPackDto>
    
    @DELETE("emoji/packs/{packId}")
    suspend fun deletePack(
        @Path("packId") packId: String
    ): Response<Unit>
    
    @POST("emoji/packs/{packId}/install")
    suspend fun installPack(
        @Path("packId") packId: String
    ): Response<Unit>
    
    @DELETE("emoji/packs/{packId}/install")
    suspend fun uninstallPack(
        @Path("packId") packId: String
    ): Response<Unit>
    
    @POST("emoji/upload/init")
    suspend fun initEmojiUpload(
        @Body request: EmojiUploadInitRequestDto
    ): Response<FileInitResponseDto>
    
    @POST("emoji/upload/confirm/{fileId}")
    suspend fun confirmEmojiUpload(
        @Path("fileId") fileId: String
    ): Response<EmojiFileDto>
}
