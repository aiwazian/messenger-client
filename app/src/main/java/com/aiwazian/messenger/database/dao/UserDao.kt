/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.aiwazian.messenger.database.entity.UserEntity
import com.aiwazian.messenger.database.entity.UserWithAvatars
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(userEntity: UserEntity)

    @Query("SELECT * FROM user WHERE id = :id")
    suspend fun get(id: Long): UserEntity?

    @Query("SELECT * FROM user WHERE id = (SELECT userId FROM account WHERE isCurrent = TRUE)")
    fun getMe(): Flow<UserEntity?>

    @Transaction
    @Query("SELECT * FROM user WHERE id = (SELECT userId FROM account WHERE isCurrent = TRUE)")
    fun getMeWithAvatars(): Flow<UserWithAvatars?>

    @Transaction
    @Query("SELECT * FROM user WHERE id = :id")
    fun getWithAvatarsFlow(id: Long): Flow<UserWithAvatars?>

    @Query("SELECT * FROM user")
    fun getAllFlow(): Flow<List<UserEntity>>
    
    @Delete
    suspend fun delete(userEntity: UserEntity)
}
