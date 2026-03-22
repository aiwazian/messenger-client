/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aiwazian.messenger.database.entity.AttachmentEntity

@Dao
interface ChatDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(attachment: AttachmentEntity)
    
    @Query("SELECT * FROM attachment WHERE id = :id")
    suspend fun get(id: Int): AttachmentEntity
    
}

