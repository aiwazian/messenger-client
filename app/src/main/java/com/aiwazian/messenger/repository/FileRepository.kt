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
            if (file.path != null && file.path != existing.path) {
                updateFilePath(file.id, file.path)
            }
            
            /*
             * Пустыми размерами ничего не затирается: сюда заходят и те, кто о кадре
             * не знает вовсе — например, завершившееся скачивание. Иначе картинка
             * теряла бы форму ровно в тот момент, когда её скачали.
             */
            if (
                file.width != null &&
                file.height != null &&
                (file.width != existing.width || file.height != existing.height)
            ) {
                updateFileDimensions(file.id, file.width, file.height)
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
    
    /**
     * Записывает размеры кадра у уже существующего файла.
     *
     * Нужно при отправке: пузырёк с картинкой появляется в чате сразу, а
     * кадр измеряется по файлу — это чтение с диска, и держать на нём показ
     * сообщения нельзя.
     */
    suspend fun updateFileDimensions(fileId: String, width: Int?, height: Int?) {
        fileDao.updateDimensions(fileId, width, height)
    }
    
    suspend fun deleteFile(fileId: String) {
        fileDao.deleteById(fileId)
    }
}
