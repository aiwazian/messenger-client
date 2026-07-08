/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.aiwazian.messenger.database.entity.ChannelEntity
import com.aiwazian.messenger.database.entity.ChannelWithAvatars
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(channelEntity: ChannelEntity)
    
    @Query("SELECT * FROM channel")
    suspend fun getAll(): List<ChannelEntity>
    
    @Query("SELECT * FROM channel WHERE id = :id")
    fun getFlow(id: Long): Flow<ChannelEntity?>
    
    @Transaction
    @Query("SELECT * FROM channel WHERE id = :id")
    fun getWithAvatarsFlow(id: Long): Flow<ChannelWithAvatars?>
    
    @Query("SELECT * FROM channel WHERE id = :id")
    suspend fun getById(id: Long): ChannelEntity?
    
    @Transaction
    @Query("SELECT * FROM channel WHERE id = :id")
    suspend fun getWithAvatars(id: Long): ChannelWithAvatars?

    @Query("DELETE FROM channel WHERE id = :id")
    suspend fun delete(id: Long)
}
