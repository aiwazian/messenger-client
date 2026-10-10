/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.aiwazian.messenger.domain.Wallpaper
import com.aiwazian.messenger.domain.WallpaperSource
import com.aiwazian.messenger.network.api.WallpaperApi
import com.aiwazian.messenger.network.dto.InitWallpaperUploadRequestDto
import com.aiwazian.messenger.network.dto.SetChatWallpaperRequestDto
import com.aiwazian.messenger.network.dto.SetWallpaperRequestDto
import com.aiwazian.messenger.network.dto.WallpaperDto
import com.aiwazian.messenger.utils.UploadManager
import com.aiwazian.messenger.utils.DataStoreManager
import com.aiwazian.messenger.utils.media.ImageCompressor
import com.aiwazian.messenger.utils.media.MediaCompressionConfig
import com.aiwazian.messenger.utils.media.MediaTransform
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WallpaperRepository @Inject constructor(
    private val wallpaperApi: WallpaperApi,
    private val uploadManager: UploadManager,
    private val imageCompressor: ImageCompressor,
    private val dataStoreManager: DataStoreManager,
    @param:ApplicationContext private val context: Context
) {
    private val _myWallpaper = MutableStateFlow<Wallpaper?>(null)
    val myWallpaper: StateFlow<Wallpaper?> = _myWallpaper.asStateFlow()

    private val _chatWallpapers = MutableStateFlow<Map<Long, Wallpaper>>(emptyMap())

    private val cacheMutex = Mutex()
    private var cacheLoaded = false
    private val json = Json { ignoreUnknownKeys = true }

    fun observeChatWallpaper(chatId: Long): Flow<Wallpaper?> =
        _chatWallpapers.map { it[chatId] }

    suspend fun refreshMyWallpaper(): Result<Unit> = withContext(Dispatchers.IO) {
        ensureCacheLoaded()

        runCatching {
            val response = wallpaperApi.getMyWallpaper()
            if (!response.isSuccessful) {
                throw IllegalStateException("Wallpaper request failed: ${response.code()}")
            }

            _myWallpaper.value = response.body()?.wallpaper?.toDomain()

            persistCache()
        }.onFailure { error ->
            Log.e(TAG, "Unable to load my wallpaper", error)
        }.map { }
    }

    suspend fun refreshChatWallpaper(chatId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        ensureCacheLoaded()

        runCatching {
            val response = wallpaperApi.getChatWallpaper(chatId)
            if (!response.isSuccessful) {
                throw IllegalStateException("Chat wallpaper request failed: ${response.code()}")
            }

            val wallpaper = response.body()?.wallpaper?.toDomain()
            _chatWallpapers.value = if (wallpaper == null) {
                _chatWallpapers.value - chatId
            } else {
                _chatWallpapers.value + (chatId to wallpaper)
            }

            persistCache()
        }.onFailure { error ->
            Log.e(TAG, "Unable to load chat wallpaper", error)
        }.map { }
    }

    suspend fun prepareDraft(
        uri: Uri,
        transform: MediaTransform,
        overlay: Bitmap?
    ): Uri? = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, DRAFT_DIRECTORY).apply { mkdirs() }

        dropStaleDrafts(directory)

        imageCompressor.compress(
            source = uri,
            directory = directory,
            maxDimension = MediaCompressionConfig.PHOTO_MAX_DIMENSION,
            quality = MediaCompressionConfig.PHOTO_JPEG_QUALITY,
            name = "${DRAFT_NAME_PREFIX}${System.currentTimeMillis()}",
            transform = transform,
            overlay = overlay
        )?.uri
    }

    suspend fun setGlobalWallpaper(
        draft: Uri,
        darken: Int,
        blur: Int,
        offsetX: Float,
        offsetY: Float
    ): Result<Unit> = withContext(Dispatchers.IO) {
        execute {
            val fileId = uploadWallpaper(draft)

            requireResponse(wallpaperApi.setMyWallpaper(SetWallpaperRequestDto(fileId, darken, blur, offsetX, offsetY)))

            refreshMyWallpaper()
        }
    }

    suspend fun setChatWallpaper(
        chatId: Long,
        scope: String,
        draft: Uri,
        darken: Int,
        blur: Int,
        offsetX: Float,
        offsetY: Float
    ): Result<Unit> = withContext(Dispatchers.IO) {
        execute {
            val fileId = uploadWallpaper(draft)

            requireResponse(
                wallpaperApi.setChatWallpaper(
                    chatId,
                    SetChatWallpaperRequestDto(scope, fileId, darken, blur, offsetX, offsetY)
                )
            )

            refreshChatWallpaper(chatId)
        }
    }

    suspend fun setChannelWallpaper(
        channelId: Long,
        draft: Uri,
        darken: Int,
        blur: Int,
        offsetX: Float,
        offsetY: Float
    ): Result<Unit> = withContext(Dispatchers.IO) {
        execute {
            val fileId = uploadWallpaper(draft)

            requireResponse(wallpaperApi.setChannelWallpaper(channelId, SetWallpaperRequestDto(fileId, darken, blur, offsetX, offsetY)))

            refreshChatWallpaper(channelId)
        }
    }

    suspend fun setGroupWallpaper(
        groupId: Long,
        draft: Uri,
        darken: Int,
        blur: Int,
        offsetX: Float,
        offsetY: Float
    ): Result<Unit> = withContext(Dispatchers.IO) {
        execute {
            val fileId = uploadWallpaper(draft)

            requireResponse(wallpaperApi.setGroupWallpaper(groupId, SetWallpaperRequestDto(fileId, darken, blur, offsetX, offsetY)))

            refreshChatWallpaper(groupId)
        }
    }

    suspend fun resetGlobalWallpaper(): Result<Unit> = withContext(Dispatchers.IO) {
        execute {
            requireResponse(wallpaperApi.resetMyWallpaper())
            _myWallpaper.value = null

            persistCache()
        }
    }

    suspend fun resetChatWallpaper(chatId: Long, scope: String = SCOPE_ME): Result<Unit> =
        withContext(Dispatchers.IO) {
            execute {
                requireResponse(wallpaperApi.resetChatWallpaper(chatId, scope))
                refreshChatWallpaper(chatId)
            }
        }

    suspend fun resetChannelWallpaper(channelId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        execute {
            requireResponse(wallpaperApi.resetChannelWallpaper(channelId))
            refreshChatWallpaper(channelId)
        }
    }

    suspend fun resetGroupWallpaper(groupId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        execute {
            requireResponse(wallpaperApi.resetGroupWallpaper(groupId))
            refreshChatWallpaper(groupId)
        }
    }

    private suspend fun execute(block: suspend () -> Unit): Result<Unit> = try {
        block()
        Result.success(Unit)
    } catch (error: Exception) {
        Log.e(TAG, "Wallpaper operation failed", error)
        Result.failure(error)
    }

    private suspend fun ensureCacheLoaded() {
        if (cacheLoaded) {
            return
        }

        cacheMutex.withLock {
            if (cacheLoaded) {
                return
            }

            val stored = dataStoreManager.getCachedWallpapers()

            if (stored != null) {
                runCatching {
                    val cache = json.decodeFromString<WallpaperCache>(stored)

                    _myWallpaper.value = cache.global?.toDomain()
                    _chatWallpapers.value = cache.chats.mapValues { it.value.toDomain() }
                }.onFailure { error ->
                    Log.e(TAG, "Unable to restore cached wallpapers", error)
                }
            }

            cacheLoaded = true
        }
    }

    private suspend fun persistCache() {
        cacheMutex.withLock {
            val cache = WallpaperCache(
                global = _myWallpaper.value?.toCached(),
                chats = _chatWallpapers.value.mapValues { it.value.toCached() }
            )

            runCatching {
                dataStoreManager.saveCachedWallpapers(json.encodeToString(cache))
            }.onFailure { error ->
                Log.e(TAG, "Unable to persist cached wallpapers", error)
            }
        }
    }

    private suspend fun uploadWallpaper(draft: Uri): String {
        val file = draft.path?.let(::File) ?: throw IllegalStateException("Wallpaper draft is missing")
        val size = file.length()

        if (size <= 0) {
            throw IllegalStateException("Wallpaper draft is empty")
        }

        val imageSize = readImageSize(draft)

        val initResponse = wallpaperApi.initUpload(
            InitWallpaperUploadRequestDto(
                name = file.name,
                size = size,
                mimeType = JPEG_MIME_TYPE,
                width = imageSize?.first,
                height = imageSize?.second
            )
        )

        if (!initResponse.isSuccessful) {
            throw IllegalStateException("Wallpaper upload init failed: ${initResponse.code()}")
        }

        val upload = initResponse.body() ?: throw IllegalStateException("Wallpaper upload init is empty")

        uploadManager.upload(
            fileUri = draft,
            upload = upload,
            fileId = upload.fileId,
            maxAttempts = UploadManager.UNLIMITED_ATTEMPTS,
            keepLocalCopy = false
        ).getOrThrow()

        requireResponse(wallpaperApi.confirmUpload(upload.fileId))

        return upload.fileId
    }

    private fun readImageSize(uri: Uri): Pair<Int, Int>? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }

        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        } catch (error: Exception) {
            Log.w(TAG, "Unable to read wallpaper dimensions", error)
        }

        if (options.outWidth <= 0 || options.outHeight <= 0) {
            return null
        }

        return options.outWidth to options.outHeight
    }

    private fun WallpaperDto.toDomain(): Wallpaper = Wallpaper(
        fileId = fileId,
        url = url,
        darken = darken,
        blur = blur,
        offsetX = offsetX,
        offsetY = offsetY,
        source = source?.let { parsed ->
            WallpaperSource.entries.firstOrNull { it.name == parsed }
        } ?: WallpaperSource.USER
    )

    private fun CachedWallpaper.toDomain(): Wallpaper = Wallpaper(
        fileId = fileId,
        url = url,
        darken = darken,
        blur = blur,
        offsetX = offsetX,
        offsetY = offsetY,
        source = WallpaperSource.entries.firstOrNull { it.name == source } ?: WallpaperSource.USER
    )

    private fun Wallpaper.toCached(): CachedWallpaper = CachedWallpaper(
        fileId = fileId,
        url = url,
        darken = darken,
        blur = blur,
        offsetX = offsetX,
        offsetY = offsetY,
        source = source.name
    )

    private fun dropStaleDrafts(directory: File) {
        val deadline = System.currentTimeMillis() - DRAFT_MAX_AGE_MS

        directory.listFiles()?.forEach { file ->
            if (file.lastModified() < deadline) {
                file.delete()
            }
        }
    }

    private fun <T> requireResponse(response: Response<T>) {
        if (!response.isSuccessful) {
            throw IllegalStateException("Wallpaper request failed: ${response.code()}")
        }
    }

    private companion object {
        const val TAG = "WallpaperRepository"
        const val DRAFT_DIRECTORY = "wallpapers"
        const val DRAFT_NAME_PREFIX = "wallpaper_"
        const val DRAFT_MAX_AGE_MS = 6L * 60 * 60 * 1000
        const val JPEG_MIME_TYPE = "image/jpeg"
        const val SCOPE_ME = "me"
    }
}

@Serializable
private data class CachedWallpaper(
    val fileId: String,
    val url: String,
    val darken: Int,
    val blur: Int,
    val offsetX: Float,
    val offsetY: Float,
    val source: String
)

@Serializable
private data class WallpaperCache(
    val global: CachedWallpaper? = null,
    val chats: Map<Long, CachedWallpaper> = emptyMap()
)
