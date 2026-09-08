/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.api

import com.aiwazian.messenger.network.dto.CreateStickerPackRequestDto
import com.aiwazian.messenger.network.dto.FileInitRequestDto
import com.aiwazian.messenger.network.dto.FileInitResponseDto
import com.aiwazian.messenger.network.dto.StickerFileDto
import com.aiwazian.messenger.network.dto.StickerPackDto
import com.aiwazian.messenger.network.dto.StickerPackUsernameAvailabilityDto
import com.aiwazian.messenger.network.dto.UpdateStickerPackRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Наборы стикеров.
 *
 * Созданные и добавленные — разные адреса, а не один с фильтром: это два
 * отдельных экрана со своим состоянием и своим порядком.
 *
 * Идентификатор набора везде строка: на сервере это BigInt.
 */
interface StickerApi {
    
    @GET("stickers/packs/created")
    suspend fun getCreatedPacks(): Response<List<StickerPackDto>>
    
    @GET("stickers/packs/added")
    suspend fun getAddedPacks(): Response<List<StickerPackDto>>
    
    /**
     * Свободно ли имя набора.
     *
     * @param packId текущий набор при редактировании: своё же имя не должно
     * считаться занятым.
     */
    @GET("stickers/packs/username-available")
    suspend fun checkUsername(
        @Query("username") username: String,
        @Query("packId") packId: String? = null
    ): Response<StickerPackUsernameAvailabilityDto>
    
    /** Набор по ссылке — предпросмотр перед добавлением к себе. */
    @GET("stickers/packs/by-username/{username}")
    suspend fun getPackByUsername(
        @Path("username") username: String
    ): Response<StickerPackDto>
    
    @GET("stickers/packs/{packId}")
    suspend fun getPack(
        @Path("packId") packId: String
    ): Response<StickerPackDto>
    
    @POST("stickers/packs")
    suspend fun createPack(
        @Body request: CreateStickerPackRequestDto
    ): Response<StickerPackDto>
    
    @PATCH("stickers/packs/{packId}")
    suspend fun updatePack(
        @Path("packId") packId: String,
        @Body request: UpdateStickerPackRequestDto
    ): Response<StickerPackDto>
    
    /** Удаляет набор у всех: доступно только создателю. */
    @DELETE("stickers/packs/{packId}")
    suspend fun deletePack(
        @Path("packId") packId: String
    ): Response<Unit>
    
    @POST("stickers/packs/{packId}/install")
    suspend fun installPack(
        @Path("packId") packId: String
    ): Response<Unit>
    
    /** Убирает набор только у себя. */
    @DELETE("stickers/packs/{packId}/install")
    suspend fun uninstallPack(
        @Path("packId") packId: String
    ): Response<Unit>
    
    /**
     * Подписанная форма для загрузки картинки стикера.
     *
     * Категорию и каталог ставит сервер: стикеры ложатся в публичный каталог,
     * и выбирать его клиенту нельзя.
     */
    @POST("stickers/upload/init")
    suspend fun initStickerUpload(
        @Body request: FileInitRequestDto
    ): Response<FileInitResponseDto>
    
    /** Файл доехал в хранилище и годен к добавлению в набор. */
    @POST("stickers/upload/confirm/{fileId}")
    suspend fun confirmStickerUpload(
        @Path("fileId") fileId: String
    ): Response<StickerFileDto>
}
