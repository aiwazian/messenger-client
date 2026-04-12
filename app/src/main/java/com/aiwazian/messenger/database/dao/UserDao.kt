/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aiwazian.messenger.database.entity.UserEntity

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(userEntity: UserEntity)

    @Query("SELECT * FROM user WHERE id = :id")
    suspend fun get(id: Long): UserEntity?

    @Query("SELECT * FROM user WHERE id = (SELECT userId FROM account WHERE isCurrent = 1)")
    suspend fun getMe(): UserEntity?

    @Query("SELECT * FROM user")
    fun getAllFlow(): kotlinx.coroutines.flow.Flow<List<UserEntity>>
    
    @Delete
    suspend fun delete(userEntity: UserEntity)
}
