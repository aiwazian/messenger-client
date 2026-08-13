/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.aiwazian.messenger.database.entity.AvatarEntity

@Dao
interface AvatarDao {
    
    /**
     * Сохранить аватарки и убрать те, которых больше нет у владельца.
     *
     * Сервер всегда отдаёт полный список аватарок профиля, значит всё, чего в пачке
     * нет, уже удалено или заменено. Раньше строки только добавлялись, и старая
     * аватарка оставалась в кэше навсегда: приложение показывало её и продолжало
     * просить у сервера файл, которого уже нет.
     *
     * Пустая пачка ничего не чистит — по ней нельзя понять владельца, поэтому случай
     * «аватарок вообще не осталось» решается явным deleteAvatarsBy*Id в репозитории.
     */
    @Transaction
    suspend fun insertAvatars(avatars: List<AvatarEntity>) {
        if (avatars.isEmpty()) {
            return
        }
        
        upsertAvatars(avatars)
        
        val actualFileIds = avatars.map { it.fileId }
        
        avatars.mapNotNull { it.userId }.distinct().forEach { userId ->
            deleteUserAvatarsNotIn(userId, actualFileIds)
        }
        
        avatars.mapNotNull { it.groupId }.distinct().forEach { groupId ->
            deleteGroupAvatarsNotIn(groupId, actualFileIds)
        }
        
        avatars.mapNotNull { it.channelId }.distinct().forEach { channelId ->
            deleteChannelAvatarsNotIn(channelId, actualFileIds)
        }
    }
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAvatars(avatars: List<AvatarEntity>)
    
    @Query("DELETE FROM avatars WHERE userId = :userId AND fileId NOT IN (:actualFileIds)")
    suspend fun deleteUserAvatarsNotIn(userId: Long, actualFileIds: List<String>)
    
    @Query("DELETE FROM avatars WHERE groupId = :groupId AND fileId NOT IN (:actualFileIds)")
    suspend fun deleteGroupAvatarsNotIn(groupId: Long, actualFileIds: List<String>)
    
    @Query("DELETE FROM avatars WHERE channelId = :channelId AND fileId NOT IN (:actualFileIds)")
    suspend fun deleteChannelAvatarsNotIn(channelId: Long, actualFileIds: List<String>)
    
    @Query("DELETE FROM avatars WHERE userId = :userId")
    suspend fun deleteAvatarsByUserId(userId: Long)
    
    @Query("DELETE FROM avatars WHERE groupId = :groupId")
    suspend fun deleteAvatarsByGroupId(groupId: Long)
    
    @Query("DELETE FROM avatars WHERE channelId = :channelId")
    suspend fun deleteAvatarsByChannelId(channelId: Long)
    
    @Query("DELETE FROM avatars WHERE fileId = :fileId")
    suspend fun deleteAvatarByFileId(fileId: String)
    
    @Query("SELECT * FROM avatars WHERE userId = :userId ORDER BY sortOrder ASC")
    suspend fun getAvatarsByUserId(userId: Long): List<AvatarEntity>
}
