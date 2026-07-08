/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.aiwazian.messenger.database.entity.ChatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    
    @Query(
        """
        SELECT * FROM chats 
        WHERE userId = :userId
        ORDER BY isPinned DESC
    """
    )
    fun getAllChatsFlow(userId: Long): Flow<List<ChatEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChats(chats: List<ChatEntity>)
    
    @Query("DELETE FROM chats WHERE userId = :userId AND chatId = :chatId")
    suspend fun deleteChat(userId: Long, chatId: Long)
    
    @Query("DELETE FROM chats WHERE userId = :userId AND chatId NOT IN (:chatIds)")
    suspend fun deleteChatsNotIn(userId: Long, chatIds: List<Long>)
    
    @Query("SELECT * FROM chats WHERE userId = :userId AND chatId = :chatId")
    fun getChatByIdFlow(userId: Long, chatId: Long): Flow<ChatEntity?>
    
    @Query("SELECT * FROM chats WHERE userId = :userId AND chatId = :chatId")
    suspend fun getChatById(userId: Long, chatId: Long): ChatEntity?
    
    @Query("DELETE FROM chats WHERE userId = :userId")
    suspend fun deleteAllChats(userId: Long)
    
    @Query("UPDATE chats SET isPinned = :isPinned WHERE userId = :userId AND chatId IN (:chatIds)")
    suspend fun updatePinnedStatus(userId: Long, chatIds: List<Long>, isPinned: Boolean)
}
