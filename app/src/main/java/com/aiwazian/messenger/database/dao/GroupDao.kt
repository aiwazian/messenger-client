/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aiwazian.messenger.database.entity.GroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(groupEntity: GroupEntity)
    
    @Query("SELECT * FROM `group` WHERE id = :id")
    fun get(id: Long): Flow<GroupEntity?>
    
    @Query("DELETE FROM `group` WHERE id = :id")
    suspend fun delete(id: Long)
}
