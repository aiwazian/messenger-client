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
    @Query("SELECT * FROM message " +
                   "WHERE senderId = :senderId AND chatId = :chatId " +
                   "OR senderId = :chatId AND chatId = :senderId " +
                   "ORDER BY sendTime ASC " +
                   "LIMIT :limit " +
                   "OFFSET :offset")
    fun getMessagesWithAttachments(
        senderId: Long, chatId: Long, limit: Int, offset: Int
    ): Flow<List<MessageWithAttachments>>
    
    @Transaction
    @Query("SELECT * FROM message " +
                   "WHERE chatId = :chatId " +
                   "OR senderId = :chatId " +
                   "ORDER BY sendTime ASC " +
                   "LIMIT :limit " +
                   "OFFSET :offset")
    fun getMessagesWithAttachments(
        chatId: Long, limit: Int, offset: Int
    ): Flow<List<MessageWithAttachments>>
    
    @Transaction
    @Query("SELECT * FROM message WHERE id = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: Long): MessageWithAttachments?
    
    @Transaction
    @Query(
        "SELECT * FROM message " +
                "WHERE (senderId = :userId AND chatId = :chatId) " +
                "OR (chatId = :userId AND senderId = :chatId) " +
                "ORDER BY sendTime DESC " +
                "LIMIT 1"
    )
    fun getChatLastMessageFlow(userId: Long, chatId: Long): Flow<MessageWithAttachments?>
    
    @Transaction
    @Query(
        "SELECT * FROM message " +
                "WHERE chatId = :chatId " +
                "ORDER BY sendTime DESC " +
                "LIMIT 1"
    )
    fun getChatLastMessageFlow(chatId: Long): Flow<MessageWithAttachments?>
    
    @Query(
        "DELETE FROM message " +
                "WHERE (senderId = :userId AND chatId = :chatId) " +
                "OR (senderId = :chatId AND chatId = :userId) " +
                "OR (senderId = :chatId AND chatId = :chatId)"
    )
    suspend fun clearChatHistory(userId: Long, chatId: Long)
    
    @Query("DELETE FROM message WHERE id = :id")
    suspend fun deleteMessageById(id: Long)
    
    @Query("UPDATE message SET id = :newId WHERE id = :oldId")
    suspend fun updateMessageId(oldId: Long, newId: Long)
    
    @Query("DELETE FROM message WHERE chatId NOT IN (:chatIds)")
    suspend fun deleteMessagesNotInChatIds(chatIds: List<Long>)
    
    @Query(
        "DELETE FROM message " +
                "WHERE ((senderId = :userId AND chatId = :chatId) " +
                "OR (senderId = :chatId AND chatId = :userId) " +
                "OR (chatId = :chatId)) " +
                "AND sendTime >= :minTime AND sendTime <= :maxTime " +
                "AND id > 0 AND id NOT IN (:ids)"
    )
    suspend fun deleteMessagesInRangeExcluding(
        userId: Long,
        chatId: Long,
        minTime: Long,
        maxTime: Long,
        ids: List<Long>
    )
    
    @Query("DELETE FROM message")
    suspend fun deleteAll()
    
    @Query("UPDATE message SET isRead = :isRead WHERE id = :id")
    suspend fun updateMessageReadStatus(id: Int, isRead: Boolean)
}
