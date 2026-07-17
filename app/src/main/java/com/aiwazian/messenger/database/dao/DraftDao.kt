/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.aiwazian.messenger.database.entity.DraftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftDao {
    
    @Query("SELECT * FROM drafts WHERE userId = :userId AND chatId = :chatId")
    fun getDraftFlow(userId: Long, chatId: Long): Flow<DraftEntity?>
    
    @Query("SELECT * FROM drafts WHERE userId = :userId")
    fun getAllDraftsFlow(userId: Long): Flow<List<DraftEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDraft(draft: DraftEntity)
    
    @Query("DELETE FROM drafts WHERE userId = :userId AND chatId = :chatId")
    suspend fun deleteDraft(userId: Long, chatId: Long)
    
    @Query("DELETE FROM drafts WHERE userId = :userId")
    suspend fun deleteAllDrafts(userId: Long)
}
