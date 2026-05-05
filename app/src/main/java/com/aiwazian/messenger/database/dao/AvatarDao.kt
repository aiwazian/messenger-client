/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aiwazian.messenger.database.entity.AvatarEntity

@Dao
interface AvatarDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAvatars(avatars: List<AvatarEntity>)

    @Query("DELETE FROM avatars WHERE userId = :userId")
    suspend fun deleteAvatarsByUserId(userId: Long)

    @Query("DELETE FROM avatars WHERE fileId = :fileId")
    suspend fun deleteAvatarByFileId(fileId: String)

    @Query("SELECT * FROM avatars WHERE userId = :userId ORDER BY sortOrder ASC")
    suspend fun getAvatarsByUserId(userId: Long): List<AvatarEntity>
}
