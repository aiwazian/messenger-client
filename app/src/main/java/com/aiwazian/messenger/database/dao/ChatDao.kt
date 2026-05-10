/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aiwazian.messenger.database.entity.ChatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    
    @Query(
        """
        SELECT * FROM chats 
        ORDER BY isPinned DESC
    """
    )
    fun getAllChatsFlow(): Flow<List<ChatEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChats(chats: List<ChatEntity>)
    
    @Query("DELETE FROM chats WHERE chatId = :chatId")
    suspend fun deleteChat(chatId: Long)
    
    @Query("DELETE FROM chats WHERE chatId NOT IN (:chatIds)")
    suspend fun deleteChatsNotIn(chatIds: List<Long>)
    
    @Query("SELECT * FROM chats WHERE chatId = :chatId")
    fun getChatByIdFlow(chatId: Long): Flow<ChatEntity?>
    
    @Query("DELETE FROM chats")
    suspend fun deleteAllChats()

    @Query("UPDATE chats SET isPinned = :isPinned WHERE chatId IN (:chatIds)")
    suspend fun updatePinnedStatus(chatIds: List<Long>, isPinned: Boolean)
}
