/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aiwazian.messenger.database.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

import com.aiwazian.messenger.database.entity.MessageWithAttachments

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMessages(messages: List<MessageEntity>)

    @androidx.room.Transaction
    @Query("SELECT * FROM (SELECT * FROM message WHERE chatId = :chatId ORDER BY sendTime DESC LIMIT :limit OFFSET :offset) ORDER BY sendTime ASC")
    fun getMessages(chatId: Long, limit: Int, offset: Int): Flow<List<MessageWithAttachments>>

    @androidx.room.Transaction
    @Query("SELECT * FROM message WHERE chatId = :chatId ORDER BY sendTime ASC")
    fun getMessages(chatId: Long): Flow<List<MessageWithAttachments>>

    @androidx.room.Transaction
    @Query("SELECT * FROM message WHERE chatId = :chatId ORDER BY sendTime ASC")
    suspend fun getMessagesSync(chatId: Long): List<MessageWithAttachments>

    @Query("SELECT DISTINCT chatId FROM message")
    suspend fun getAllChatIds(): List<Long>

    @androidx.room.Transaction
    @Query("SELECT * FROM message WHERE id = :messageId LIMIT 1")
    suspend fun getMessageWithAttachmentsById(messageId: Int): MessageWithAttachments?

    @Query("SELECT * FROM message WHERE id = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: Int): MessageEntity?

    @Query("DELETE FROM message WHERE chatId = :chatId")
    suspend fun clearChatHistory(chatId: Long)
}
