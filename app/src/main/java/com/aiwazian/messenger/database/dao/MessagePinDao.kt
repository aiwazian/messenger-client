package com.aiwazian.messenger.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.aiwazian.messenger.database.entity.MessagePinEntity
import kotlinx.coroutines.flow.Flow

/**
 * Закрепления сообщений, разделённые по аккаунтам.
 *
 * Условие по владельцу берётся подзапросом из таблицы account — как и в
 * MessageDao: активный аккаунт там уже хранится, и это единственный источник
 * правды.
 */
@Dao
interface MessagePinDao {

    @Query(
        "SELECT * FROM message_pin " +
                "WHERE chatId = :chatId " +
                "AND ownerId = " +
                "(SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1) " +
                "ORDER BY pinnedAt DESC"
    )
    fun observeChatPins(chatId: Long): Flow<List<MessagePinEntity>>

    @Upsert
    suspend fun upsertPins(pins: List<MessagePinEntity>)

    @Query(
        "DELETE FROM message_pin " +
                "WHERE chatId = :chatId " +
                "AND ownerId = " +
                "(SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1)"
    )
    suspend fun deleteChatPins(chatId: Long)

    @Query(
        "DELETE FROM message_pin " +
                "WHERE chatId = :chatId AND messageId = :messageId AND forEveryone = :forEveryone " +
                "AND ownerId = " +
                "(SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1)"
    )
    suspend fun deletePin(chatId: Long, messageId: Long, forEveryone: Boolean)

    @Query("DELETE FROM message_pin WHERE messageId = :messageId")
    suspend fun deleteByMessageId(messageId: Long)

    @Query("DELETE FROM message_pin")
    suspend fun deleteAll()
}
