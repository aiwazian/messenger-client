/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aiwazian.messenger.database.entity.FileEntity
import com.aiwazian.messenger.enums.DownloadStatus

@Dao
interface FileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(file: FileEntity)

    @Query("SELECT * FROM file WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): FileEntity?

    @Query("UPDATE file SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: DownloadStatus)
    
    @Query("UPDATE file SET path = :path WHERE id = :id")
    suspend fun updatePath(id: String, path: String?)
    
    @Query("SELECT * FROM file")
    suspend fun getAllFiles(): List<FileEntity>
    
    @Query("UPDATE file SET id = :newId WHERE id = :oldId")
    suspend fun updateFileId(oldId: String, newId: String)
    
    @Query("DELETE FROM file WHERE id = :fileId")
    suspend fun deleteFile(fileId: String)
}
