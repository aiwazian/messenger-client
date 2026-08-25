/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import android.util.Log
import androidx.core.net.toUri
import com.aiwazian.messenger.database.entity.FileEntity
import com.aiwazian.messenger.domain.ChatMediaItem
import com.aiwazian.messenger.domain.ChatMediaPage
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.network.api.ChatMediaApi
import com.aiwazian.messenger.network.dto.ChatMediaItemDto
import com.aiwazian.messenger.network.dto.ChatMediaResponseDto
import retrofit2.Response
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Галерея чата: вложения берутся с сервера, а состояние загрузки — из Room.
 *
 * Сервер не знает, что из переписки уже лежит на устройстве, поэтому путь и
 * статус доклеиваются здесь: скачанное показывается сразу и второй раз не качается.
 *
 * Кэш файлов читается одним запросом на страницу, а не по файлу на элемент:
 * шестьдесят походов в базу на одну страницу сетки заметны на скролле.
 */
@Singleton
class ChatMediaRepository @Inject constructor(
    private val chatMediaApi: ChatMediaApi,
    private val fileRepository: FileRepository
) {
    
    /** Фото и видео чата, от новых к старым. */
    suspend fun getMedia(
        chatId: Long,
        cursorId: Int? = null,
        limit: Int = PAGE_SIZE
    ): Result<ChatMediaPage> = load { chatMediaApi.getChatMedia(chatId, cursorId, limit) }
    
    /** Документы чата, от новых к старым. */
    suspend fun getFiles(
        chatId: Long,
        cursorId: Int? = null,
        limit: Int = PAGE_SIZE
    ): Result<ChatMediaPage> = load { chatMediaApi.getChatFiles(chatId, cursorId, limit) }
    
    private suspend fun load(
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
            
            val cached = fileRepository.getAllFiles().associateBy { it.id }
            
            Result.success(
                ChatMediaPage(
                    items = body.items.map { it.toDomain(cached[it.fileId]) },
                    nextCursorId = body.nextCursorId
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Не удалось загрузить вложения чата", e)
            Result.failure(e)
        }
    }
    
    /**
     * Свежее состояние файлов для уже загруженного списка.
     *
     * Нужно после скачивания: список уже на экране, и запрашивать его у сервера
     * заново ради появившегося локального файла незачем.
     */
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
    
    private fun ChatMediaItemDto.toDomain(cached: FileEntity?) = ChatMediaItem(
        id = id,
        fileId = fileId,
        messageId = messageId,
        name = name,
        size = if (size > 0) size else cached?.size ?: 0L,
        mimeType = mimeType,
        type = type,
        sendTime = sendTime,
        status = cached?.status ?: DownloadStatus.IDLE,
        localUri = cached.localUri()
    )
    
    /**
     * Скачанные файлы лежат абсолютным путём, отправленные — готовым uri.
     *
     * Обе формы должны дать один и тот же адрес, иначе Coil откажется показывать
     * только что скачанную картинку.
     */
    private fun FileEntity?.localUri() = this?.path
        ?.takeIf { it.isNotBlank() }
        ?.let { path ->
            if (path.startsWith('/')) File(path).toUri() else path.toUri()
        }
    
    companion object {
        /** Шесть рядов сетки по три столбца: с запасом на один экран вперёд. */
        const val PAGE_SIZE = 60
        
        private const val TAG = "ChatMediaRepository"
    }
}
