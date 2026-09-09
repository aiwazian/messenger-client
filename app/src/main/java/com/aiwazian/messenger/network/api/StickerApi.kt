package com.aiwazian.messenger.network.api

import com.aiwazian.messenger.network.dto.CreateStickerPackRequestDto
import com.aiwazian.messenger.network.dto.FileInitResponseDto
import com.aiwazian.messenger.network.dto.StickerFileDto
import com.aiwazian.messenger.network.dto.StickerPackDto
import com.aiwazian.messenger.network.dto.StickerPackIdDto
import com.aiwazian.messenger.network.dto.StickerPackUsernameAvailabilityDto
import com.aiwazian.messenger.network.dto.StickerUploadInitRequestDto
import com.aiwazian.messenger.network.dto.UpdateStickerPackRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface StickerApi {
    
    @GET("stickers/packs/created")
    suspend fun getCreatedPacks(): Response<List<StickerPackDto>>
    
    @GET("stickers/packs/added")
    suspend fun getAddedPacks(
        @Query("includeStickers") includeStickers: Boolean? = null
    ): Response<List<StickerPackDto>>
    
    @GET("stickers/packs/username-available")
    suspend fun checkUsername(
        @Query("username") username: String,
        @Query("packId") packId: String? = null
    ): Response<StickerPackUsernameAvailabilityDto>
    
    @GET("stickers/packs/by-username/{username}")
    suspend fun getPackByUsername(
        @Path("username") username: String
    ): Response<StickerPackDto>
    
    @GET("stickers/packs/{packId}")
    suspend fun getPack(
        @Path("packId") packId: String
    ): Response<StickerPackDto>
    
    @POST("stickers/packs/reserve")
    suspend fun reservePackId(): Response<StickerPackIdDto>
    
    @POST("stickers/packs")
    suspend fun createPack(
        @Body request: CreateStickerPackRequestDto
    ): Response<StickerPackDto>
    
    @PATCH("stickers/packs/{packId}")
    suspend fun updatePack(
        @Path("packId") packId: String,
        @Body request: UpdateStickerPackRequestDto
    ): Response<StickerPackDto>
    
    @DELETE("stickers/packs/{packId}")
    suspend fun deletePack(
        @Path("packId") packId: String
    ): Response<Unit>
    
    @POST("stickers/packs/{packId}/install")
    suspend fun installPack(
        @Path("packId") packId: String
    ): Response<Unit>
    
    @DELETE("stickers/packs/{packId}/install")
    suspend fun uninstallPack(
        @Path("packId") packId: String
    ): Response<Unit>
    
    @POST("stickers/upload/init")
    suspend fun initStickerUpload(
        @Body request: StickerUploadInitRequestDto
    ): Response<FileInitResponseDto>
    
    @POST("stickers/upload/confirm/{fileId}")
    suspend fun confirmStickerUpload(
        @Path("fileId") fileId: String
    ): Response<StickerFileDto>
}
