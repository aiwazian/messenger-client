/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.aiwazian.messenger.database.entity.FileEntity
import com.aiwazian.messenger.enums.DownloadStatus

@Dao
interface FileDao {
    @Upsert
    suspend fun save(file: FileEntity)

    @Query("SELECT * FROM file WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): FileEntity?

    @Query("UPDATE file SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: DownloadStatus)
    
    @Query("UPDATE file SET path = :path WHERE id = :id")
    suspend fun updatePath(id: String, path: String?)
    
    @Query("UPDATE file SET size = :size WHERE id = :id")
    suspend fun updateSize(id: String, size: Long)
    
    @Query("SELECT * FROM file")
    suspend fun getAllFiles(): List<FileEntity>
    
    @Query("UPDATE file SET id = :newId WHERE id = :oldId")
    suspend fun updateFileId(oldId: String, newId: String)
    
    @Query("DELETE FROM file WHERE id = :fileId")
    suspend fun deleteById(fileId: String)
}
