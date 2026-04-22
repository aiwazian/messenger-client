/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import com.aiwazian.messenger.database.AppDatabase
import com.aiwazian.messenger.database.dao.AttachmentDao
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.ui.screens.settings.storage.CategoryStats
import com.aiwazian.messenger.ui.screens.settings.storage.FileCategory
import com.aiwazian.messenger.ui.screens.settings.storage.StorageFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepository @Inject constructor(
    private val appDatabase: AppDatabase,
    private val attachmentDao: AttachmentDao
) {
    
    suspend fun clearDatabaseExceptAccount() = withContext(Dispatchers.IO) {
        appDatabase.messageDao().deleteAll()
    }
    
    suspend fun getStorageStats(): List<CategoryStats> = withContext(Dispatchers.IO) {
        val allAttachments = attachmentDao.getAllAttachments()
        
        val categoryMap = mutableMapOf<FileCategory, Pair<Int, Long>>()
        
        allAttachments.forEach { attachment ->
            if (attachment.localUri != null) {
                val file = File(attachment.localUri)
                if (file.exists()) {
                    val category = FileCategory.fromExtension(attachment.extension)
                    val current = categoryMap[category] ?: (0 to 0L)
                    categoryMap[category] = (current.first + 1) to (current.second + attachment.size)
                }
            }
        }
        
        FileCategory.entries.map { category ->
            val stats = categoryMap[category] ?: (0 to 0L)
            CategoryStats(
                category = category,
                fileCount = stats.first,
                totalSize = stats.second,
                isSelected = false
            )
        }
    }
    
    suspend fun getFilesForCategories(categories: List<FileCategory>): List<StorageFile> =
        withContext(Dispatchers.IO) {
            val allAttachments = attachmentDao.getAllAttachments()
            
            allAttachments.filter { attachment ->
                attachment.localUri != null &&
                        FileCategory.fromExtension(attachment.extension) in categories &&
                        File(attachment.localUri).exists()
            }.map { attachment ->
                StorageFile(
                    id = attachment.id,
                    name = attachment.name,
                    size = attachment.size,
                    extension = attachment.extension,
                    category = FileCategory.fromExtension(attachment.extension),
                    localUri = attachment.localUri!!,
                    messageId = if (attachment.type == AttachmentType.FILE) attachment.relationId.toInt() else 0,
                    chatId = attachment.chatId ?: 0
                )
            }
        }
    
    suspend fun clearFiles(files: List<StorageFile>) = withContext(Dispatchers.IO) {
        files.forEach { storageFile ->
            val file = File(storageFile.localUri)
            if (file.exists()) {
                file.delete()
            }

            attachmentDao.updateAttachmentStatus(
                id = storageFile.id,
                status = DownloadStatus.IDLE,
                progress = 0,
                localUri = null
            )
        }
    }
}
