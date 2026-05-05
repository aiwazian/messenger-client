package com.aiwazian.messenger.repository

import com.aiwazian.messenger.database.dao.FileDao
import com.aiwazian.messenger.database.entity.FileEntity
import com.aiwazian.messenger.enums.DownloadStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepository @Inject constructor(
    private val fileDao: FileDao
) {
    suspend fun save(file: FileEntity) {
        fileDao.save(file)
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

    suspend fun deleteFile(fileId: String) {
        fileDao.deleteFile(fileId)
    }
}
