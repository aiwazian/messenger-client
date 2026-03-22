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

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMessages(messages: List<MessageEntity>)

    @Query("SELECT * FROM message WHERE chatId = :chatId ORDER BY sendTime ASC")
    fun getMessages(chatId: Long): Flow<List<MessageEntity>>

    @Query("DELETE FROM message WHERE chatId = :chatId")
    suspend fun clearChatHistory(chatId: Long)
}
