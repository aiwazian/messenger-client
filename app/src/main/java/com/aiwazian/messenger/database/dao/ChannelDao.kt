/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aiwazian.messenger.database.entity.ChannelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(channelEntity: ChannelEntity)
    
    @Query("SELECT * FROM channel")
    suspend fun getAll(): List<ChannelEntity>
    
    @Query("SELECT * FROM channel WHERE id = :id")
    fun getFlow(id: Long): Flow<ChannelEntity?>
    
    @Query("SELECT * FROM channel WHERE id = :id")
    suspend fun get(id: Long): ChannelEntity?
    
    @Query("SELECT * FROM channel")
    fun getAllFlow(): Flow<List<ChannelEntity>>
    
    @Update
    suspend fun update(channelEntity: ChannelEntity)
    
    @Query("DELETE FROM 'channel' WHERE id = :id")
    suspend fun delete(id: Long)
}
