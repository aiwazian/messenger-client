/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import android.util.Log
import androidx.core.net.toUri
import com.aiwazian.messenger.database.dao.ChatMediaDao
import com.aiwazian.messenger.database.entity.ChatMediaCountsEntity
import com.aiwazian.messenger.database.entity.ChatMediaEntity
import com.aiwazian.messenger.database.entity.FileEntity
import com.aiwazian.messenger.database.entity.VoiceDurationEntity
import com.aiwazian.messenger.domain.ChatMediaCounts
import com.aiwazian.messenger.domain.ChatMediaItem
import com.aiwazian.messenger.domain.ChatMediaPage
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.network.api.ChatMediaApi
import com.aiwazian.messenger.network.dto.ChatMediaItemDto
import com.aiwazian.messenger.network.dto.ChatMediaResponseDto
import retrofit2.Response
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatMediaRepository @Inject constructor(
    private val chatMediaApi: ChatMediaApi,
    private val chatMediaDao: ChatMediaDao,
    private val fileRepository: FileRepository
) {
    
    suspend fun getCachedMedia(
        chatId: Long,
        limit: Int = PAGE_SIZE
    ): List<ChatMediaItem> = cached(chatId, MEDIA_TYPES, limit)
    
    suspend fun getCachedFiles(
        chatId: Long,
        limit: Int = PAGE_SIZE
    ): List<ChatMediaItem> = cached(chatId, FILE_TYPES, limit)
    
    suspend fun getCachedVoices(
        chatId: Long,
        limit: Int = PAGE_SIZE
    ): List<ChatMediaItem> = cached(chatId, VOICE_TYPES, limit)
    
    suspend fun getMedia(
        chatId: Long,
        cursorId: Int? = null,
        limit: Int = PAGE_SIZE
    ): Result<ChatMediaPage> = load(chatId, MEDIA_TYPES, cursorId) {
        chatMediaApi.getChatMedia(chatId, cursorId, limit)
    }
    
    suspend fun getFiles(
        chatId: Long,
        cursorId: Int? = null,
        limit: Int = PAGE_SIZE
    ): Result<ChatMediaPage> = load(chatId, FILE_TYPES, cursorId) {
        chatMediaApi.getChatFiles(chatId, cursorId, limit)
    }
    
    suspend fun getVoices(
        chatId: Long,
        cursorId: Int? = null,
        limit: Int = PAGE_SIZE
    ): Result<ChatMediaPage> = load(chatId, VOICE_TYPES, cursorId) {
        chatMediaApi.getChatVoices(chatId, cursorId, limit)
    }
    
    suspend fun getCachedCounts(chatId: Long): ChatMediaCounts? {
        return try {
            chatMediaDao.getCounts(chatId)?.let { counts ->
                ChatMediaCounts(
                    photos = counts.photos,
                    videos = counts.videos,
                    files = counts.files,
                    voices = counts.voices
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Не удалось прочитать кэш счётчиков галереи", e)
            null
        }
    }
    
    suspend fun getCounts(chatId: Long): Result<ChatMediaCounts> {
        return try {
            val response = chatMediaApi.getChatMediaCounts(chatId)
            val body = response.body()
            
            if (!response.isSuccessful || body == null) {
                return Result.failure(
                    Exception("Failed to load chat media counts: ${response.code()}")
                )
            }
            
            chatMediaDao.saveCounts(
                ChatMediaCountsEntity(
                    chatId = chatId,
                    photos = body.photos,
                    videos = body.videos,
                    files = body.files,
                    voices = body.voices
                )
            )
            
            Result.success(
                ChatMediaCounts(
                    photos = body.photos,
                    videos = body.videos,
                    files = body.files,
                    voices = body.voices
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Не удалось загрузить счётчики галереи", e)
            Result.failure(e)
        }
    }
    
    suspend fun saveVoiceDuration(fileId: String, durationMs: Int) {
        try {
            chatMediaDao.upsertVoiceDuration(
                VoiceDurationEntity(fileId = fileId, durationMs = durationMs)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Не удалось сохранить длину голосового", e)
        }
    }
    
    suspend fun withLocalState(items: List<ChatMediaItem>): List<ChatMediaItem> {
        if (items.isEmpty()) return items
        
        val cached = fileRepository.getAllFiles().associateBy { it.id }
        
        return items.map { item ->
            val file = cached[item.fileId]
            
            item.copy(
                status = file?.status ?: DownloadStatus.IDLE,
                localUri = file.localUri()
            )
        }
    }
    
    private suspend fun cached(
        chatId: Long,
        types: List<String>,
        limit: Int
    ): List<ChatMediaItem> {
        return try {
            val rows = chatMediaDao.getByTypes(chatId, types, limit)
            
            if (rows.isEmpty()) {
                return emptyList()
            }
            
            val files = fileRepository.getAllFiles().associateBy { it.id }
            val durations = durationsOf(rows.map { it.fileId })
            
            rows.map { it.toDomain(files[it.fileId], durations[it.fileId]) }
        } catch (e: Exception) {
            Log.e(TAG, "Не удалось прочитать кэш вложений чата", e)
            emptyList()
        }
    }
    
    private suspend fun load(
        chatId: Long,
        types: List<String>,
        cursorId: Int?,
        request: suspend () -> Response<ChatMediaResponseDto>
    ): Result<ChatMediaPage> {
        return try {
            val response = request()
            val body = response.body()
            
            if (!response.isSuccessful || body == null) {
                return Result.failure(
                    Exception("Failed to load chat attachments: ${response.code()}")
                )
            }
            
            if (cursorId == null) {
                chatMediaDao.saveWindow(chatId, types, body.items.map { it.toEntity(chatId) })
            }
            
            val cached = fileRepository.getAllFiles().associateBy { it.id }
            val durations = durationsOf(body.items.map { it.fileId })
            
            Result.success(
                ChatMediaPage(
                    items = body.items.map { it.toDomain(cached[it.fileId], durations[it.fileId]) },
                    nextCursorId = body.nextCursorId
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Не удалось загрузить вложения чата", e)
            Result.failure(e)
        }
    }
    
    private suspend fun durationsOf(fileIds: List<String>): Map<String, Int> {
        if (fileIds.isEmpty()) {
            return emptyMap()
        }
        
        return chatMediaDao.getVoiceDurations(fileIds).associate { it.fileId to it.durationMs }
    }
    
    private fun ChatMediaItemDto.toEntity(chatId: Long) = ChatMediaEntity(
        id = id,
        chatId = chatId,
        fileId = fileId,
        messageId = messageId,
        senderId = senderId,
        name = name,
        size = size,
        mimeType = mimeType,
        type = type,
        sendTime = sendTime
    )
    
    private fun ChatMediaItemDto.toDomain(cached: FileEntity?, durationMs: Int?) = ChatMediaItem(
        id = id,
        fileId = fileId,
        messageId = messageId,
        name = name,
        size = if (size > 0) size else cached?.size ?: 0L,
        mimeType = mimeType,
        type = type,
        sendTime = sendTime,
        senderId = senderId,
        status = cached?.status ?: DownloadStatus.IDLE,
        localUri = cached.localUri(),
        durationMs = durationMs
    )
    
    private fun ChatMediaEntity.toDomain(cached: FileEntity?, durationMs: Int?) = ChatMediaItem(
        id = id,
        fileId = fileId,
        messageId = messageId,
        name = name,
        size = if (size > 0) size else cached?.size ?: 0L,
        mimeType = mimeType,
        type = type,
        sendTime = sendTime,
        senderId = senderId,
        status = cached?.status ?: DownloadStatus.IDLE,
        localUri = cached.localUri(),
        durationMs = durationMs
    )
    
    private fun FileEntity?.localUri() = this?.path
        ?.takeIf { it.isNotBlank() }
        ?.let { path ->
            if (path.startsWith('/')) File(path).toUri() else path.toUri()
        }
    
    companion object {
        const val PAGE_SIZE = 60
        
        private val MEDIA_TYPES = listOf(
            AttachmentType.IMAGE.name,
            AttachmentType.VIDEO.name,
            AttachmentType.GIF.name
        )
        
        private val FILE_TYPES = listOf(AttachmentType.FILE.name)
        
        private val VOICE_TYPES = listOf(AttachmentType.VOICE.name)
        
        private const val TAG = "ChatMediaRepository"
    }
}
