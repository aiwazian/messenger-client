/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.aiwazian.messenger.database.entity.GroupEntity
import com.aiwazian.messenger.database.entity.GroupWithAvatars
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(groupEntity: GroupEntity)
    
    @Query("SELECT * FROM `group` WHERE id = :id")
    fun get(id: Long): Flow<GroupEntity?>
    
    @Transaction
    @Query("SELECT * FROM `group` WHERE id = :id")
    fun getWithAvatarsFlow(id: Long): Flow<GroupWithAvatars?>
    
    @Query("DELETE FROM `group` WHERE id = :id")
    suspend fun delete(id: Long)
}
