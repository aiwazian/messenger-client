/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.aiwazian.messenger.database.entity.UserEntity
import com.aiwazian.messenger.database.entity.UserWithAvatars
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(userEntity: UserEntity)

    @Query("SELECT * FROM user WHERE id = (SELECT userId FROM account WHERE isCurrent = TRUE)")
    fun getMe(): Flow<UserEntity?>

    @Transaction
    @Query("SELECT * FROM user WHERE id = (SELECT userId FROM account WHERE isCurrent = TRUE)")
    fun getMeWithAvatars(): Flow<UserWithAvatars?>

    @Transaction
    @Query("SELECT * FROM user WHERE id = :id")
    fun getWithAvatarsFlow(id: Long): Flow<UserWithAvatars?>
    
    @Query("SELECT * FROM user WHERE id = :id")
    suspend fun getById(id: Long): UserEntity?
    
    @Transaction
    @Query("SELECT * FROM user WHERE id = :id")
    suspend fun getWithAvatars(id: Long): UserWithAvatars?
}
