/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import android.util.Log
import com.aiwazian.messenger.di.FileClient
import com.aiwazian.messenger.domain.StickerDraft
import com.aiwazian.messenger.domain.StickerPack
import com.aiwazian.messenger.mappers.toDomain
import com.aiwazian.messenger.network.api.StickerApi
import com.aiwazian.messenger.network.dto.CreateStickerPackRequestDto
import com.aiwazian.messenger.network.dto.FileInitRequestDto
import com.aiwazian.messenger.network.dto.StickerInputDto
import com.aiwazian.messenger.network.dto.UpdateStickerPackRequestDto
import com.aiwazian.messenger.utils.media.EncodedSticker
import com.aiwazian.messenger.utils.media.MediaCompressionConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StickerRepository @Inject constructor(
    private val stickerApi: StickerApi,
    @param:FileClient private val fileClient: OkHttpClient
) {
    
    suspend fun getCreatedPacks(): Result<List<StickerPack>> = withContext(Dispatchers.IO) {
        request("created packs") {
            stickerApi.getCreatedPacks()
        }.map { packs -> packs.map { it.toDomain() } }
    }
    
    suspend fun getAddedPacks(): Result<List<StickerPack>> = withContext(Dispatchers.IO) {
        request("added packs") {
            stickerApi.getAddedPacks()
        }.map { packs -> packs.map { it.toDomain() } }
    }
    
    suspend fun getPack(packId: Long): Result<StickerPack> = withContext(Dispatchers.IO) {
        request("pack $packId") {
            stickerApi.getPack(packId.toString())
        }.map { it.toDomain() }
    }
    
    suspend fun getPackByUsername(username: String): Result<StickerPack> =
        withContext(Dispatchers.IO) {
            request("pack @$username") {
                stickerApi.getPackByUsername(username)
            }.map { it.toDomain() }
        }
    
    suspend fun isUsernameAvailable(username: String, packId: Long? = null): Result<Boolean> =
        withContext(Dispatchers.IO) {
            request("username @$username") {
                stickerApi.checkUsername(username, packId?.toString())
            }.map { it.available }
        }
    
    suspend fun createPack(
        name: String,
        username: String,
        stickers: List<StickerDraft>
    ): Result<StickerPack> = withContext(Dispatchers.IO) {
        request("pack creation") {
            stickerApi.createPack(
                CreateStickerPackRequestDto(
                    name = name,
                    username = username,
                    stickers = stickers.map {
                        StickerInputDto(fileId = it.fileId, emojis = it.emojis)
                    })
            )
        }.map { it.toDomain() }
    }
    
    suspend fun updatePack(
        packId: Long,
        name: String? = null,
        username: String? = null,
        stickers: List<StickerDraft>? = null
    ): Result<StickerPack> = withContext(Dispatchers.IO) {
        request("pack $packId update") {
            stickerApi.updatePack(
                packId.toString(),
                UpdateStickerPackRequestDto(
                    name = name,
                    username = username,
                    stickers = stickers?.map {
                        StickerInputDto(fileId = it.fileId, emojis = it.emojis)
                    })
            )
        }.map { it.toDomain() }
    }
    
    suspend fun deletePack(packId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        requestUnit("pack $packId removal") {
            stickerApi.deletePack(packId.toString())
        }
    }
    
    suspend fun installPack(packId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        requestUnit("pack $packId install") {
            stickerApi.installPack(packId.toString())
        }
    }
    
    suspend fun uninstallPack(packId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        requestUnit("pack $packId uninstall") {
            stickerApi.uninstallPack(packId.toString())
        }
    }
    
    suspend fun uploadSticker(sticker: EncodedSticker): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val path = sticker.uri.path
                
                if (path.isNullOrEmpty()) {
                    return@withContext Result.failure(Exception("Sticker file is missing"))
                }
                
                val file = File(path)
                
                if (!file.exists()) {
                    return@withContext Result.failure(Exception("Sticker file is missing"))
                }
                
                val initResponse = stickerApi.initStickerUpload(
                    FileInitRequestDto(
                        name = sticker.name,
                        size = sticker.size,
                        mimeType = sticker.mimeType,
                        width = MediaCompressionConfig.STICKER_SIZE,
                        height = MediaCompressionConfig.STICKER_SIZE
                    )
                )
                
                val form = initResponse.body()
                
                if (!initResponse.isSuccessful || form == null) {
                    return@withContext Result.failure(
                        Exception("Sticker upload init failed ${initResponse.code()}")
                    )
                }
                
                if (form.maxSizeBytes > 0 && sticker.size > form.maxSizeBytes) {
                    return@withContext Result.failure(Exception("Sticker is too large"))
                }
                
                val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
                
                form.fields.forEach { (key, value) ->
                    builder.addFormDataPart(key, value)
                }
                
                builder.addFormDataPart(
                    FILE_FIELD_NAME,
                    sticker.name,
                    file.asRequestBody(sticker.mimeType.toMediaTypeOrNull())
                )
                
                val request = Request.Builder().url(form.url).post(builder.build()).build()
                
                fileClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            Exception("Storage rejected the sticker ${response.code}")
                        )
                    }
                }
                
                val confirmResponse = stickerApi.confirmStickerUpload(form.fileId)
                
                if (!confirmResponse.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("Sticker upload confirm failed ${confirmResponse.code()}")
                    )
                }
                
                Result.success(form.fileId)
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading a sticker", e)
                
                Result.failure(e)
            }
        }
    
    private suspend fun <T> request(
        what: String,
        call: suspend () -> retrofit2.Response<T>
    ): Result<T> {
        return try {
            val response = call()
            val body = response.body()
            
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception("Failed to load $what ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading $what", e)
            
            Result.failure(e)
        }
    }
    
    private suspend fun requestUnit(
        what: String,
        call: suspend () -> retrofit2.Response<Unit>
    ): Result<Unit> {
        return try {
            val response = call()
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to perform $what ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error performing $what", e)
            
            Result.failure(e)
        }
    }
    
    private companion object {
        const val TAG = "StickerRepository"
        
        const val FILE_FIELD_NAME = "file"
    }
}
