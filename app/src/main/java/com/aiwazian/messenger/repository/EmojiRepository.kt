package com.aiwazian.messenger.repository

import android.util.Log
import com.aiwazian.messenger.database.dao.EmojiDao
import com.aiwazian.messenger.di.FileClient
import com.aiwazian.messenger.domain.CustomEmojiDraft
import com.aiwazian.messenger.domain.EmojiPack
import com.aiwazian.messenger.mappers.toDomain
import com.aiwazian.messenger.mappers.toEntity
import com.aiwazian.messenger.network.api.EmojiApi
import com.aiwazian.messenger.network.dto.CreateEmojiPackRequestDto
import com.aiwazian.messenger.network.dto.EmojiInputDto
import com.aiwazian.messenger.network.dto.EmojiUploadInitRequestDto
import com.aiwazian.messenger.network.dto.UpdateEmojiPackRequestDto
import com.aiwazian.messenger.utils.media.EncodedEmoji
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
class EmojiRepository @Inject constructor(
    private val emojiApi: EmojiApi,
    private val emojiDao: EmojiDao,
    @param:FileClient private val fileClient: OkHttpClient
) {
    
    suspend fun getCreatedPacks(): Result<List<EmojiPack>> = withContext(Dispatchers.IO) {
        val result = request("created emoji packs") {
            emojiApi.getCreatedPacks()
        }.map { packs -> packs.map { it.toDomain() } }
        
        val packs = result.getOrNull() ?: return@withContext cachedOwnedPacks().takeIf {
            it.isNotEmpty()
        }?.let { Result.success(it) } ?: result
        
        packs.forEach { pack -> cachePack(pack) }
        
        result
    }
    
    suspend fun getAddedPacks(includeEmojis: Boolean = false): Result<List<EmojiPack>> =
        withContext(Dispatchers.IO) {
            val result = request("added emoji packs") {
                emojiApi.getAddedPacks(if (includeEmojis) true else null)
            }.map { packs -> packs.map { it.toDomain() } }
            
            val packs = result.getOrNull() ?: return@withContext cachedInstalledPacks().takeIf {
                it.isNotEmpty()
            }?.let { Result.success(it) } ?: result
            
            emojiDao.clearInstalled()
            
            packs.forEachIndexed { index, pack -> cachePack(pack, index) }
            
            result
        }
    
    suspend fun getPack(packId: Long): Result<EmojiPack> = withContext(Dispatchers.IO) {
        val result = request("emoji pack $packId") {
            emojiApi.getPack(packId.toString())
        }.map { it.toDomain() }
        
        val pack = result.getOrNull() ?: return@withContext cachedPack(packId)?.let {
            Result.success(it)
        } ?: result
        
        cachePack(pack)
        
        result
    }
    
    suspend fun getPackByUsername(username: String): Result<EmojiPack> =
        withContext(Dispatchers.IO) {
            val result = request("emoji pack @$username") {
                emojiApi.getPackByUsername(username)
            }.map { it.toDomain() }
            
            val pack = result.getOrNull() ?: return@withContext cachedPackByUsername(username)?.let {
                Result.success(it)
            } ?: result
            
            cachePack(pack)
            
            result
        }
    
    suspend fun isUsernameAvailable(username: String, packId: Long? = null): Result<Boolean> =
        withContext(Dispatchers.IO) {
            request("emoji pack username @$username") {
                emojiApi.checkUsername(username, packId?.toString())
            }.map { it.available }
        }
    
    suspend fun reservePackId(): Result<Long> = withContext(Dispatchers.IO) {
        request("emoji pack id") {
            emojiApi.reservePackId()
        }.mapCatching { dto ->
            dto.packId.toLongOrNull() ?: throw Exception("Server returned an invalid pack id")
        }
    }
    
    suspend fun createPack(
        packId: Long,
        name: String,
        username: String,
        emojis: List<CustomEmojiDraft>,
        coverFileId: String? = null
    ): Result<EmojiPack> = withContext(Dispatchers.IO) {
        val result = request("emoji pack creation") {
            emojiApi.createPack(
                CreateEmojiPackRequestDto(
                    id = packId.toString(),
                    name = name,
                    username = username,
                    coverFileId = coverFileId,
                    emojis = emojis.map {
                        EmojiInputDto(fileId = it.fileId, emojis = it.emojis)
                    })
            )
        }.map { it.toDomain() }
        
        result.getOrNull()?.let { pack -> cachePack(pack) }
        
        result
    }
    
    suspend fun updatePack(
        packId: Long,
        name: String? = null,
        username: String? = null,
        emojis: List<CustomEmojiDraft>? = null,
        coverFileId: String? = null,
        removeCover: Boolean = false
    ): Result<EmojiPack> = withContext(Dispatchers.IO) {
        val result = request("emoji pack $packId update") {
            emojiApi.updatePack(
                packId.toString(),
                UpdateEmojiPackRequestDto(
                    name = name,
                    username = username,
                    coverFileId = coverFileId,
                    removeCover = if (removeCover) true else null,
                    emojis = emojis?.map {
                        EmojiInputDto(fileId = it.fileId, emojis = it.emojis)
                    })
            )
        }.map { it.toDomain() }
        
        result.getOrNull()?.let { pack -> cachePack(pack) }
        
        result
    }
    
    suspend fun deletePack(packId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val result = requestUnit("emoji pack $packId removal") {
            emojiApi.deletePack(packId.toString())
        }
        
        if (result.isSuccess) {
            emojiDao.deleteEmojis(packId)
            emojiDao.deletePack(packId)
        }
        
        result
    }
    
    suspend fun installPack(packId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val result = requestUnit("emoji pack $packId install") {
            emojiApi.installPack(packId.toString())
        }
        
        if (result.isSuccess) {
            emojiDao.setInstalled(packId, true)
        }
        
        result
    }
    
    suspend fun uninstallPack(packId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val result = requestUnit("emoji pack $packId uninstall") {
            emojiApi.uninstallPack(packId.toString())
        }
        
        if (result.isSuccess) {
            emojiDao.setInstalled(packId, false)
        }
        
        result
    }
    
    suspend fun uploadEmoji(emoji: EncodedEmoji, packId: Long): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val path = emoji.uri.path
                
                if (path.isNullOrEmpty()) {
                    return@withContext Result.failure(Exception("Emoji file is missing"))
                }
                
                val file = File(path)
                
                if (!file.exists()) {
                    return@withContext Result.failure(Exception("Emoji file is missing"))
                }
                
                val initResponse = emojiApi.initEmojiUpload(
                    EmojiUploadInitRequestDto(
                        name = emoji.name,
                        size = emoji.size,
                        mimeType = emoji.mimeType,
                        packId = packId.toString(),
                        width = MediaCompressionConfig.EMOJI_SIZE,
                        height = MediaCompressionConfig.EMOJI_SIZE
                    )
                )
                
                val form = initResponse.body()
                
                if (!initResponse.isSuccessful || form == null) {
                    return@withContext Result.failure(
                        Exception("Emoji upload init failed ${initResponse.code()}")
                    )
                }
                
                if (form.maxSizeBytes > 0 && emoji.size > form.maxSizeBytes) {
                    return@withContext Result.failure(Exception("Emoji is too large"))
                }
                
                val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
                
                form.fields.forEach { (key, value) ->
                    builder.addFormDataPart(key, value)
                }
                
                builder.addFormDataPart(
                    FILE_FIELD_NAME,
                    emoji.name,
                    file.asRequestBody(emoji.mimeType.toMediaTypeOrNull())
                )
                
                val request = Request.Builder().url(form.url).post(builder.build()).build()
                
                fileClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            Exception("Storage rejected the emoji ${response.code}")
                        )
                    }
                }
                
                val confirmResponse = emojiApi.confirmEmojiUpload(form.fileId)
                
                if (!confirmResponse.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("Emoji upload confirm failed ${confirmResponse.code()}")
                    )
                }
                
                Result.success(form.fileId)
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading an emoji", e)
                
                Result.failure(e)
            }
        }
    
    private suspend fun cachePack(pack: EmojiPack, sortOrder: Int? = null) {
        val order = sortOrder ?: emojiDao.getPack(pack.id)?.sortOrder ?: 0
        
        emojiDao.upsertPacks(listOf(pack.toEntity(order)))
        
        if (pack.emojis.isEmpty()) {
            return
        }
        
        emojiDao.deleteEmojis(pack.id)
        emojiDao.upsertEmojis(pack.emojis.map { it.toEntity(pack.id) })
    }
    
    private suspend fun cachedPack(packId: Long): EmojiPack? =
        emojiDao.getPack(packId)?.let { entity -> entity.toDomain(cachedEmojis(entity.id)) }
    
    private suspend fun cachedPackByUsername(username: String): EmojiPack? =
        emojiDao.getPackByUsername(username.trim().lowercase())
            ?.let { entity -> entity.toDomain(cachedEmojis(entity.id)) }
    
    private suspend fun cachedInstalledPacks(): List<EmojiPack> =
        emojiDao.getInstalledPacks().map { entity ->
            entity.toDomain(cachedEmojis(entity.id))
        }
    
    private suspend fun cachedOwnedPacks(): List<EmojiPack> =
        emojiDao.getOwnedPacks().map { entity ->
            entity.toDomain(cachedEmojis(entity.id))
        }
    
    private suspend fun cachedEmojis(packId: Long) =
        emojiDao.getEmojis(packId).map { it.toDomain() }
    
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
        const val TAG = "EmojiRepository"
        
        const val FILE_FIELD_NAME = "file"
    }
}
