/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
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
    
    @Query("SELECT * FROM `group` WHERE id = :id")
    suspend fun getById(id: Long): GroupEntity?
    
    @Transaction
    @Query("SELECT * FROM `group` WHERE id = :id")
    suspend fun getWithAvatars(id: Long): GroupWithAvatars?

    @Query("DELETE FROM `group` WHERE id = :id")
    suspend fun delete(id: Long)
}
