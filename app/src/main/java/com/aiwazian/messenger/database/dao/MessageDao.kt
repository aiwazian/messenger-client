/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.aiwazian.messenger.database.entity.MessageEntity
import com.aiwazian.messenger.database.entity.MessageWithAttachments
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMessages(messages: List<MessageEntity>)
    
    @Transaction
    @Query("SELECT * FROM (SELECT * FROM message WHERE senderId = :userId AND chatId = :chatId OR senderId = :chatId AND chatId = :userId ORDER BY sendTime DESC LIMIT :limit OFFSET :offset) ORDER BY sendTime ASC")
    fun getMessages(
        userId: Long,
        chatId: Long,
        limit: Int,
        offset: Int
    ): Flow<List<MessageWithAttachments>>
    
    @Transaction
    @Query("SELECT * FROM (SELECT * FROM message WHERE chatId = :chatId ORDER BY sendTime DESC LIMIT :limit OFFSET :offset) ORDER BY sendTime ASC")
    fun getMessages(chatId: Long, limit: Int, offset: Int): Flow<List<MessageWithAttachments>>
    
    @Query("SELECT * FROM message WHERE id = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: Int): MessageEntity?
    
    @Query(
        "DELETE FROM message " +
                "WHERE senderId != 0 " +
                "AND (senderId = :userId AND chatId = :chatId) " +
                "OR (senderId = :chatId AND chatId = :userId) " +
                "OR (senderId = :chatId AND chatId = :chatId)"
    )
    suspend fun clearChatHistory(userId: Long, chatId: Long)
    
    @Query("DELETE FROM message WHERE id = :id")
    suspend fun deleteMessageById(id: Int)
    
    @Query("DELETE FROM message WHERE chatId NOT IN (:chatIds)")
    suspend fun deleteMessagesNotInChatIds(chatIds: List<Long>)
    
    @Query("DELETE FROM message")
    suspend fun deleteAll()
}
