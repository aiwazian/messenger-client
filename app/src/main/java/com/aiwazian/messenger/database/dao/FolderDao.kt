package com.aiwazian.messenger.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aiwazian.messenger.database.entity.FolderEntity

@Dao
interface FolderDao {
    @Query("SELECT * FROM folder")
    suspend fun getAll(): List<FolderEntity>
    
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertAll(folderEntities: List<FolderEntity>)
    
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(folderEntities: FolderEntity)
    
    @Delete
    suspend fun delete(folderEntity: FolderEntity)
}