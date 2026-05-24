/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import com.aiwazian.messenger.database.dao.FileDao
import com.aiwazian.messenger.database.entity.FileEntity
import com.aiwazian.messenger.enums.DownloadStatus
import javax.inject.Inject

class FileRepository @Inject constructor(
    private val fileDao: FileDao
) {
    suspend fun upsert(file: FileEntity) {
        val existing = getById(file.id)
        if (existing == null) {
            fileDao.save(file)
        } else {
            updateFileSize(file.id, file.size)
            updateFileStatus(file.id, file.status)
            if (existing.path == null && file.path != null) {
                updateFilePath(file.id, file.path)
            }
        }
    }
    
    suspend fun getById(id: String): FileEntity? {
        return fileDao.getById(id)
    }
    
    suspend fun getAllFiles(): List<FileEntity> {
        return fileDao.getAllFiles()
    }
    
    suspend fun updateFileId(oldId: String, newId: String) {
        fileDao.updateFileId(oldId, newId)
    }
    
    suspend fun updateFileStatus(fileId: String, status: DownloadStatus) {
        fileDao.updateStatus(fileId, status)
    }
    
    suspend fun updateFilePath(fileId: String, path: String?) {
        fileDao.updatePath(fileId, path)
    }
    
    suspend fun updateFileSize(fileId: String, size: Long) {
        fileDao.updateSize(fileId, size)
    }
    
    suspend fun deleteFile(fileId: String) {
        fileDao.deleteById(fileId)
    }
}
