/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import android.util.Log
import com.aiwazian.messenger.database.AppDatabase
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.ui.screens.settings.storage.CacheClearResult
import com.aiwazian.messenger.ui.screens.settings.storage.CategoryStats
import com.aiwazian.messenger.ui.screens.settings.storage.FileCategory
import com.aiwazian.messenger.ui.screens.settings.storage.StorageFile
import com.aiwazian.messenger.utils.media.StickerCacheStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepository @Inject constructor(
    private val appDatabase: AppDatabase,
    private val fileRepository: FileRepository,
    private val stickerCacheStorage: StickerCacheStorage
) {

    suspend fun clearDatabaseExceptAccount() {
        appDatabase.messageDao().deleteAll()
    }

    suspend fun getStorageStats(): List<CategoryStats> = withContext(Dispatchers.IO) {
        val allFiles = fileRepository.getAllFiles()

        val categoryMap = mutableMapOf<FileCategory, Pair<Int, Long>>()

        allFiles.forEach { fileEntity ->
            if (fileEntity.status == DownloadStatus.COMPLETED && fileEntity.path != null) {
                val file = File(fileEntity.path)
                if (file.exists()) {
                    val extension = fileEntity.name.substringAfterLast('.', "")
                    val category = FileCategory.fromExtension(extension)
                    val current = categoryMap[category] ?: (0 to 0L)
                    categoryMap[category] = (current.first + 1) to (current.second + fileEntity.size)
                }
            }
        }

        FileCategory.entries.map { category ->
            val stats = categoryMap[category] ?: (0 to 0L)
            val totalSize = if (category == FileCategory.EMOJI_STICKERS) {
                stats.second + stickerCacheStorage.getCacheSize()
            } else {
                stats.second
            }
            CategoryStats(
                category = category,
                fileCount = stats.first,
                totalSize = totalSize,
                isSelected = false
            )
        }
    }
    
    suspend fun getFilesForCategories(categories: List<FileCategory>): List<StorageFile> =
        withContext(Dispatchers.IO) {
            val allFiles = fileRepository.getAllFiles()
            
            allFiles.filter { fileEntity ->
                fileEntity.status == DownloadStatus.COMPLETED &&
                        fileEntity.path != null &&
                        FileCategory.fromExtension(
                            fileEntity.name.substringAfterLast('.', "")
                        ) in categories &&
                        File(fileEntity.path).exists()
            }.map { fileEntity ->
                StorageFile(
                    id = fileEntity.id,
                    name = fileEntity.name,
                    size = fileEntity.size,
                    extension = fileEntity.name.substringAfterLast('.', ""),
                    category = FileCategory.fromExtension(
                        fileEntity.name.substringAfterLast('.', "")
                    ),
                    localUri = fileEntity.path!!,
                    messageId = 0,
                    chatId = 0
                )
            }
        }
    
    suspend fun clearCache(categories: List<FileCategory>): CacheClearResult =
        withContext(Dispatchers.IO) {
            val filesResult = clearFiles(getFilesForCategories(categories))

            if (!categories.contains(FileCategory.EMOJI_STICKERS)) {
                return@withContext filesResult
            }

            val stickersResult = try {
                CacheClearResult(freedBytes = stickerCacheStorage.clear())
            } catch (e: Exception) {
                Log.e(TAG, "Unable to clear sticker caches", e)
                CacheClearResult()
            }

            CacheClearResult(
                freedBytes = filesResult.freedBytes + stickersResult.freedBytes,
                failedCount = filesResult.failedCount + stickersResult.failedCount
            )
        }

    suspend fun clearFiles(files: List<StorageFile>): CacheClearResult =
        withContext(Dispatchers.IO) {
            var freedBytes = 0L
            var failedCount = 0
            
            files.forEach { storageFile ->
                val file = File(storageFile.localUri)
                val exists = file.exists()
                
                val size = if (exists) file.length() else 0L
                
                if (exists && !file.delete()) {
                    failedCount++
                    return@forEach
                }
                
                freedBytes += size
                fileRepository.deleteFile(fileId = storageFile.id)
            }
            
            CacheClearResult(freedBytes = freedBytes, failedCount = failedCount)
        }

    private companion object {
        const val TAG = "StorageRepository"
    }
}
