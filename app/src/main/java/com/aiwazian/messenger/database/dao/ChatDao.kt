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
    
    /**
     * Пришло chat:unread — берём счётчик с сервера как есть.
     * Сервер — единственный источник истины, иначе два устройства разъедутся.
     */
    @Query(
        """
        UPDATE chats 
        SET unreadCount = :unreadCount, 
            firstUnreadMessageId = :firstUnreadMessageId, 
            isManuallyUnread = :isManuallyUnread 
        WHERE userId = :userId AND chatId = :chatId
    """
    )
    suspend fun setUnreadState(
        userId: Long,
        chatId: Long,
        unreadCount: Int,
        firstUnreadMessageId: Long?,
        isManuallyUnread: Boolean
    )
    
    /**
     * Локальный инкремент на случай, если message:new опередило chat:unread.
     * firstUnreadMessageId выставляется только если был пуст: граница не должна съехать вниз.
     *
     * Ручная пометка снимается: появились настоящие непрочитанные, бейдж должен показать число.
     */
    @Query(
        """
        UPDATE chats 
        SET unreadCount = unreadCount + 1, 
            firstUnreadMessageId = COALESCE(firstUnreadMessageId, :messageId), 
            isManuallyUnread = 0 
        WHERE userId = :userId AND chatId = :chatId
    """
    )
    suspend fun incrementUnread(userId: Long, chatId: Long, messageId: Long)
    
    @Query(
        """
        UPDATE chats 
        SET unreadCount = 0, firstUnreadMessageId = NULL, isManuallyUnread = 0 
        WHERE userId = :userId AND chatId = :chatId
    """
    )
    suspend fun clearUnread(userId: Long, chatId: Long)
    
    /** Пункт «Пометить непрочитанным» в меню выделения: счётчик не трогаем. */
    @Query(
        """
        UPDATE chats 
        SET isManuallyUnread = :isManuallyUnread 
        WHERE userId = :userId AND chatId IN (:chatIds)
    """
    )
    suspend fun setManuallyUnread(userId: Long, chatIds: List<Long>, isManuallyUnread: Boolean)
    
    /**
     * Колокольчик в ChatCard и в шапке чата.
     *
     * Пишется до ответа сервера, чтобы пункт меню срабатывал сразу, и возвращается
     * назад, если запрос не прошёл.
     */
    @Query("UPDATE chats SET isMuted = :isMuted WHERE userId = :userId AND chatId = :chatId")
    suspend fun setMuted(userId: Long, chatId: Long, isMuted: Boolean)
    
    /**
     * Сброс настроек уведомлений: исключений больше нет, категории включены —
     * молчать в списке чатов больше некому.
     */
    @Query("UPDATE chats SET isMuted = 0 WHERE userId = :userId")
    suspend fun clearMuted(userId: Long)
    
    /** null — чата нет в кэше, решать судьбу уведомления придётся по категории. */
    @Query("SELECT isMuted FROM chats WHERE userId = :userId AND chatId = :chatId")
    suspend fun isMuted(userId: Long, chatId: Long): Boolean?
}
