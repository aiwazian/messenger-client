/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import com.aiwazian.messenger.database.entity.ProxyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProxyDao {
    @Query("SELECT * FROM proxy ORDER BY id ASC")
    fun observeAll(): Flow<List<ProxyEntity>>

    @Query("SELECT * FROM proxy WHERE id = :id")
    suspend fun getById(id: Long): ProxyEntity?

    @Insert
    suspend fun insert(proxy: ProxyEntity): Long

    @Update
    suspend fun update(proxy: ProxyEntity)

    @Delete
    suspend fun delete(proxy: ProxyEntity)
}
